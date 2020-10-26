package com.novalang.parser

import com.novalang.CompileError
import com.novalang.parser.actions.DispatcherAction

class Pipeline<STAGE_TYPE : BaseStage<RESPONSE_TYPE>, RESPONSE_TYPE : BaseStageResponse> {
  private val stages: List<BaseStage<*>>
  private lateinit var lastStage: STAGE_TYPE

  private constructor(
    stages: List<BaseStage<*>>
  ) {
    this.stages = stages
  }

  private constructor(
    stages: List<BaseStage<*>>,
    lastStage: STAGE_TYPE
  ) : this(stages) {
    this.lastStage = lastStage
  }

  fun <NEW_STAGE_TYPE : BaseStage<NEW_RESPONSE_TYPE>, NEW_RESPONSE_TYPE : BaseStageResponse> thenDoStage(action: () -> NEW_STAGE_TYPE): Pipeline<NEW_STAGE_TYPE, NEW_RESPONSE_TYPE> {
    val stage = action()

    return Pipeline(
      stages = stages + stage,
      lastStage = stage
    )
  }

  fun thenDo(action: (response: RESPONSE_TYPE) -> Unit): Pipeline<STAGE_TYPE, RESPONSE_TYPE> {
    lastStage.after { action(it) }

    return this
  }

  fun thenSetState(action: (state: State) -> State): Pipeline<Stage, StageResponse> {
    val newStage = Stage { _, state, _ -> StageResponse(action(state)) }

    return Pipeline(
      stages = stages + newStage,
      lastStage = newStage
    )
  }

  fun thenExpectToken(action: (tokens: TokenData) -> Token?): Pipeline<ExpectTokenStage, ExpectTokenStageResponse> {
    return thenDoStage { ExpectTokenStage(getAction = action) }
  }

  fun thenExpectTokenCount(count: Int): Pipeline<ExpectTokenCountStage, ExpectTokenCountStageResponse> {
    return thenDoStage { ExpectTokenCountStage(count) }
  }

  fun thenDoAction(action: (state: State, tokens: TokenData) -> DispatcherAction): Pipeline<ActionStage, StageResponse> {
    return thenDoStage { ActionStage(getAction = action) }
  }

  fun thenDoAll(action: (state: State, tokens: TokenData) -> List<BaseStage<*>>): Pipeline<ReducerStage, StageResponse> {
    return thenDoStage { ReducerStage(getAction = action) }
  }

  fun orElseError(action: (response: RESPONSE_TYPE) -> String): Pipeline<STAGE_TYPE, RESPONSE_TYPE> {
    lastStage.after {
      lastStage.errorMessage = action(it)
    }

    return this
  }

  fun run(dispatcher: Dispatcher, initialState: State, tokenData: TokenData): State {
    tokenData.createSnapshot()

    val state = stages.fold(initialState) { acc, stage ->
      val response = stage.run(dispatcher, acc, tokenData)

      if (!response.success) {
        return@run if (stage.errorMessage != null) {
          val error = CompileError(
            message = stage.errorMessage!!,
            tokenData = tokenData.unconsumed()
          )

          initialState.copy(errors = initialState.errors + error)
        } else {
          tokenData.restoreSnapshot()

          initialState
        }
      }

      response.state
    }

    tokenData.dropSnapshot()

    return state
  }

  companion object {
    fun create(): Pipeline<*, *> {
      return Pipeline<BaseStage<BaseStageResponse>, BaseStageResponse>(emptyList())
    }
  }
}

abstract class BaseStage<RESPONSE_TYPE : BaseStageResponse>(
  open var errorMessage: String? = null,
  open val action: (dispatcher: Dispatcher, state: State, tokens: TokenData) -> RESPONSE_TYPE
) {
  private val beforeActions = mutableListOf<() -> Unit>()
  private val afterActions = mutableListOf<(response: RESPONSE_TYPE) -> Unit>()
  private val afterSuccessActions = mutableListOf<(response: RESPONSE_TYPE) -> Unit>()

  open fun before(action: () -> Unit): BaseStage<RESPONSE_TYPE> {
    beforeActions.add(action)

    return this
  }

  open fun after(action: (response: RESPONSE_TYPE) -> Unit): BaseStage<RESPONSE_TYPE> {
    afterActions.add(action)

    return this
  }

  open fun afterSuccess(action: (response: RESPONSE_TYPE) -> Unit): BaseStage<RESPONSE_TYPE> {
    afterSuccessActions.add(action)

    return this
  }

  fun run(dispatcher: Dispatcher, state: State, tokens: TokenData): RESPONSE_TYPE {
    beforeActions.forEach { it() }

    val response = action(dispatcher, state, tokens)

    afterActions.forEach { it(response) }

    if (response.success) {
      afterSuccessActions.forEach { it(response) }
    }

    return response
  }
}

data class Stage(
  override var errorMessage: String? = null,
  override val action: (dispatcher: Dispatcher, state: State, tokens: TokenData) -> StageResponse
) : BaseStage<StageResponse>(errorMessage, action)

data class ReducerStage(
  override var errorMessage: String? = null,
  override val action: (dispatcher: Dispatcher, state: State, tokens: TokenData) -> StageResponse = { dispatcher, state, tokens ->
    getAction(state, tokens).fold(StageResponse(state)) { acc, stage ->
      val response = stage.action(dispatcher, acc.state, tokens)

      acc.copy(
        state = acc.state,
        success = acc.success && response.success
      )
    }
  },
  val getAction: (state: State, tokens: TokenData) -> List<BaseStage<*>>
) : BaseStage<StageResponse>(errorMessage, action)

data class ExpectTokenStage(
  override var errorMessage: String? = null,
  override val action: (dispatcher: Dispatcher, state: State, tokens: TokenData) -> ExpectTokenStageResponse = { _, state, tokens ->
    val token = getAction(tokens)

    ExpectTokenStageResponse(
      state = state,
      success = token != null,
      token = token
    )
  },
  val getAction: (tokens: TokenData) -> Token?
) : BaseStage<ExpectTokenStageResponse>(errorMessage, action)

data class ExpectTokenCountStage(
  val count: Int,
  override var errorMessage: String? = null,
  override val action: (dispatcher: Dispatcher, state: State, tokens: TokenData) -> ExpectTokenCountStageResponse = { _, state, (tokens) ->
    val actual = tokens.unconsumed.size

    ExpectTokenCountStageResponse(
      state = state,
      success = count == actual,
      expected = count,
      actual = actual
    )
  }
) : BaseStage<ExpectTokenCountStageResponse>(errorMessage, action)

data class ListStage<T>(
  val items: List<T>,
  override var errorMessage: String? = null,
  override val action: (dispatcher: Dispatcher, state: State, tokens: TokenData) -> StageResponse = { _, state, tokens ->
    items.fold(StageResponse(state)) { acc, item ->
      val response = getAction(item, state, tokens)

      acc.copy(
        state = acc.state,
        success = acc.success && response.success
      )
    }
  },
  val getAction: (item: T, state: State, tokens: TokenData) -> StageResponse
) : BaseStage<StageResponse>(errorMessage, action)

data class ActionStage(
  override var errorMessage: String? = null,
  override val action: (dispatcher: Dispatcher, state: State, tokens: TokenData) -> StageResponse = { dispatcher, state, tokens ->
    StageResponse(
      dispatcher.dispatchAndExecute(
        state = state,
        action = getAction(state, tokens)
      )
    )
  },
  val getAction: (state: State, tokens: TokenData) -> DispatcherAction
) : BaseStage<StageResponse>(
  errorMessage,
  action
)

abstract class BaseStageResponse(
  open val state: State,
  open val success: Boolean = true
) {
}

data class StageResponse(
  override val state: State,
  override val success: Boolean = true
) : BaseStageResponse(
  state,
  success
)

data class ExpectTokenStageResponse(
  val token: Token?,
  override val state: State,
  override val success: Boolean = true
) : BaseStageResponse(
  state,
  success
)

data class ExpectTokenCountStageResponse(
  val expected: Int,
  val actual: Int,
  override val state: State,
  override val success: Boolean = true
) : BaseStageResponse(
  state,
  success
)