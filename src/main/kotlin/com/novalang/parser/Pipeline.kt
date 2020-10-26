package com.novalang.parser

import com.novalang.CompileError
import com.novalang.parser.actions.DispatcherAction

data class Pipeline<RESPONSE_TYPE : BaseStageResponse>(
  val stages: List<BaseStage<*>>
) {
  fun <NEW_RESPONSE_TYPE : BaseStageResponse> thenDoStage(action: () -> BaseStage<NEW_RESPONSE_TYPE>): Pipeline<NEW_RESPONSE_TYPE> {
    return Pipeline(
      stages = stages + action()
    )
  }

  fun thenDo(action: (response: RESPONSE_TYPE) -> Unit): Pipeline<RESPONSE_TYPE> {
    stages.last().after {
      action(it as RESPONSE_TYPE)
    }

    return this
  }

  fun thenSetState(action: (state: State) -> State): Pipeline<RESPONSE_TYPE> {
    return Pipeline(
      stages = stages + Stage { _, state, _ -> StageResponse(action(state)) }
    )
  }

  fun thenExpectToken(action: (tokens: TokenList) -> Token?): Pipeline<ExpectTokenStageResponse> {
    return thenDoStage { ExpectTokenStage(getAction = action) }
  }

  fun thenExpectTokenCount(count: Int): Pipeline<ExpectTokenCountStageResponse> {
    return thenDoStage { ExpectTokenCountStage(count) }
  }

  fun thenDoAction(action: (state: State, tokens: TokenList) -> DispatcherAction): Pipeline<StageResponse> {
    return thenDoStage { ActionStage(getAction = action) }
  }

  fun thenDoAll(action: (state: State, tokens: TokenList) -> List<BaseStage<*>>): Pipeline<StageResponse> {
    return thenDoStage { ReducerStage(getAction = action) }
  }

  fun orElseError(action: (response: RESPONSE_TYPE) -> String): Pipeline<RESPONSE_TYPE> {
    val stage = stages.last()

    stage.after {
      stage.errorMessage = action(it as RESPONSE_TYPE)
    }

    return this
  }

  fun run(dispatcher: Dispatcher, initialState: State, tokenData: TokenData): State {
    return stages.fold(initialState) { acc, stage ->
      val response = stage.run(dispatcher, acc, tokenData.currentTokens)

      if (!response.success) {
        return@run if (stage.errorMessage != null) {
          val error = CompileError(
            message = stage.errorMessage!!,
            tokenData = tokenData.unconsumed()
          )

          initialState.copy(errors = initialState.errors + error)
        } else {
          initialState
        }
      }

      response.state
    }
  }

  companion object {
    fun create(): Pipeline<*> {
      return Pipeline<BaseStageResponse>(emptyList())
    }
  }
}

abstract class BaseStage<RESPONSE_TYPE : BaseStageResponse>(
  open var errorMessage: String? = null,
  open val action: (dispatcher: Dispatcher, state: State, tokens: TokenList) -> RESPONSE_TYPE
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

  fun run(dispatcher: Dispatcher, state: State, tokens: TokenList): RESPONSE_TYPE {
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
  override val action: (dispatcher: Dispatcher, state: State, tokens: TokenList) -> StageResponse
) : BaseStage<StageResponse>(errorMessage, action)

data class ReducerStage(
  override var errorMessage: String? = null,
  override val action: (dispatcher: Dispatcher, state: State, tokens: TokenList) -> StageResponse = { dispatcher, state, tokens ->
    getAction(state, tokens).fold(StageResponse(state)) { acc, stage ->
      val response = stage.action(dispatcher, acc.state, tokens)

      acc.copy(
        state = acc.state,
        success = acc.success && response.success
      )
    }
  },
  val getAction: (state: State, tokens: TokenList) -> List<BaseStage<*>>
) : BaseStage<StageResponse>(errorMessage, action)

data class ExpectTokenStage(
  override var errorMessage: String? = null,
  override val action: (dispatcher: Dispatcher, state: State, tokens: TokenList) -> ExpectTokenStageResponse = { _, state, tokens ->
    val token = getAction(tokens)

    ExpectTokenStageResponse(
      state = state,
      success = token != null,
      token = token
    )
  },
  val getAction: (tokens: TokenList) -> Token?
) : BaseStage<ExpectTokenStageResponse>(errorMessage, action)

data class ExpectTokenCountStage(
  val count: Int,
  override var errorMessage: String? = null,
  override val action: (dispatcher: Dispatcher, state: State, tokens: TokenList) -> ExpectTokenCountStageResponse = { _, state, tokens ->
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
  override val action: (dispatcher: Dispatcher, state: State, tokens: TokenList) -> StageResponse = { _, state, tokens ->
    items.fold(StageResponse(state)) { acc, item ->
      val response = getAction(item, state, tokens)

      acc.copy(
        state = acc.state,
        success = acc.success && response.success
      )
    }
  },
  val getAction: (item: T, state: State, tokens: TokenList) -> StageResponse
) : BaseStage<StageResponse>(errorMessage, action)

data class ActionStage(
  override var errorMessage: String? = null,
  override val action: (dispatcher: Dispatcher, state: State, tokens: TokenList) -> StageResponse = { dispatcher, state, tokens ->
    StageResponse(
      dispatcher.dispatchAndExecute(
        state = state,
        action = getAction(state, tokens)
      )
    )
  },
  val getAction: (state: State, tokens: TokenList) -> DispatcherAction
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