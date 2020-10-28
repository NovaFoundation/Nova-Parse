package com.novalang.parser

import com.novalang.CompileError
import com.novalang.parser.actions.DispatcherAction
import com.novalang.replace
import java.lang.RuntimeException

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

  fun thenDoWithResponse(action: (response: RESPONSE_TYPE) -> Unit): Pipeline<STAGE_TYPE, RESPONSE_TYPE> {
    return thenDo { _, _, response -> action(response!!) }
  }

  fun thenDo(action: (state: State, tokens: TokenData, response: RESPONSE_TYPE?) -> Unit): Pipeline<STAGE_TYPE, RESPONSE_TYPE> {
    if (::lastStage.isInitialized) {
      lastStage.after { state, tokens, response -> action(state, tokens, response) }
    }

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

  fun orElseError(errorMessage: String): Pipeline<STAGE_TYPE, RESPONSE_TYPE> {
    return orElseError { _, _, _ -> errorMessage }
  }

  fun orElseError(action: (state: State, tokens: TokenData, response: RESPONSE_TYPE) -> String): Pipeline<STAGE_TYPE, RESPONSE_TYPE> {
    lastStage.after { state, tokens, response ->
      lastStage.errorMessage = action(state, tokens, response)
    }

    return this
  }

  fun thenCheck(action: (state: State, tokens: TokenData) -> Any?): Pipeline<ConditionStage, StageResponse> {
    val stage = ConditionStage { state, tokens ->
      action(state, tokens)
    }

    return Pipeline(
      stages = stages + stage,
      lastStage = stage
    )
  }

  fun ifEqualsAnyThenDo(vararg expectedValues: Any?, action: (state: State, tokens: TokenData) -> Unit): Pipeline<ConditionStage, StageResponse> {
    return ifTrueThenDo({ value -> expectedValues.any { it == value } }, action)
  }

  fun ifEqualsThenDo(expectedValue: Any?, action: (state: State, tokens: TokenData) -> Unit): Pipeline<ConditionStage, StageResponse> {
    return ifEqualsThenDo({ expectedValue }, action)
  }

  fun ifEqualsThenDo(expectedValue: () -> Any?, action: (state: State, tokens: TokenData) -> Unit): Pipeline<ConditionStage, StageResponse> {
    return ifEquals(expectedValue) {
      it.thenDo { state, tokens, _ -> action(state, tokens) }
    }
  }

  fun ifEquals(expectedValue: Any?, action: (pipeline: Pipeline<*, *>) -> Pipeline<*, *>): Pipeline<ConditionStage, StageResponse> {
    return ifEquals({ expectedValue }, action)
  }

  fun ifEquals(expectedValue: () -> Any?, action: (pipeline: Pipeline<*, *>) -> Pipeline<*, *>): Pipeline<ConditionStage, StageResponse> {
    return ifTrue({ it == expectedValue() }, action)
  }

  fun ifNotEquals(expectedValue: Any?, action: (pipeline: Pipeline<*, *>) -> Pipeline<*, *>): Pipeline<ConditionStage, StageResponse> {
    return ifTrue({ it != expectedValue }, action)
  }

  fun ifTrueThenDo(conditionCheck: (value: Any?) -> Boolean, action: (state: State, tokens: TokenData) -> Unit): Pipeline<ConditionStage, StageResponse> {
    return ifTrue(conditionCheck) {
      it.thenDo { state, tokens, _ -> action(state, tokens) }
    }
  }

  fun ifTrueThenDo(action: (state: State, tokens: TokenData) -> Unit): Pipeline<ConditionStage, StageResponse> {
    return ifTrue {
      it.thenDo { state, tokens, _ -> action(state, tokens) }
    }
  }

  fun ifTrueThenDoAction(action: (state: State, tokens: TokenData) -> DispatcherAction): Pipeline<ConditionStage, StageResponse> {
    return ifTrue {
      it.thenDoAction(action)
    }
  }

  fun ifTrueThenSetState(action: (state: State) -> State): Pipeline<ConditionStage, StageResponse> {
    return ifTrue {
      it.thenSetState(action)
    }
  }

  fun ifTrue(conditionCheck: (value: Any?) -> Boolean, action: (pipeline: Pipeline<*, *>) -> Pipeline<*, *>): Pipeline<ConditionStage, StageResponse> {
    if (lastStage !is ConditionStage) {
      throw RuntimeException("Must be in a conditional to check ifTrue")
    }

    val conditionStage = lastStage as ConditionStage

    val condition = { value: Any? ->
      if (conditionCheck(value)) {
        action(create())
      } else {
        null
      }
    }

    val stage = ConditionStage(
      conditions = conditionStage.conditions + condition,
      elseConditions = conditionStage.elseConditions,
      getAction = conditionStage.getAction
    )

    return Pipeline(
      stages = stages.replace(lastStage, stage),
      lastStage = stage
    )
  }

  fun ifFalse(conditionCheck: (value: Any?) -> Boolean, action: (pipeline: Pipeline<*, *>) -> Pipeline<*, *>): Pipeline<ConditionStage, StageResponse> {
    if (lastStage !is ConditionStage) {
      throw RuntimeException("Must be in a conditional to check ifFalse")
    }

    val conditionStage = lastStage as ConditionStage

    val condition = { value: Any? ->
      if (!conditionCheck(value)) {
        action(create())
      } else {
        null
      }
    }

    val stage = ConditionStage(
      conditions = conditionStage.conditions + condition,
      elseConditions = conditionStage.elseConditions,
      getAction = conditionStage.getAction
    )

    return Pipeline(
      stages = stages.replace(lastStage, stage),
      lastStage = stage
    )
  }

  fun ifTrue(action: (pipeline: Pipeline<*, *>) -> Pipeline<*, *>): Pipeline<ConditionStage, StageResponse> {
    return ifEquals(true, action)
  }

  fun ifFalse(action: (pipeline: Pipeline<*, *>) -> Pipeline<*, *>): Pipeline<ConditionStage, StageResponse> {
    return ifEquals(false, action)
  }

  fun ifFalseThenDo(conditionCheck: (value: Any?) -> Boolean, action: (state: State, tokens: TokenData) -> Unit): Pipeline<ConditionStage, StageResponse> {
    return ifFalse(conditionCheck) {
      it.thenDo { state, tokens, _ -> action(state, tokens) }
    }
  }

  fun ifFalseThenDo(action: (state: State, tokens: TokenData) -> Unit): Pipeline<ConditionStage, StageResponse> {
    return ifFalse {
      it.thenDo { state, tokens, _ -> action(state, tokens) }
    }
  }

  fun ifFalseThenDoAction(action: (state: State, tokens: TokenData) -> DispatcherAction): Pipeline<ConditionStage, StageResponse> {
    return ifFalse {
      it.thenDoAction(action)
    }
  }

  fun ifFalseThenSetState(action: (state: State) -> State): Pipeline<ConditionStage, StageResponse> {
    return ifFalse {
      it.thenSetState(action)
    }
  }

  fun orElseExit(): Pipeline<ConditionStage, StageResponse> {
    return orElse {
      create()
        .thenDo { state, _, _ ->
          StageResponse(
            state = state,
            success = false
          )
        }
    }
  }

  fun orElseThenDo(action: (state: State, tokens: TokenData) -> Unit): Pipeline<ConditionStage, StageResponse> {
    return orElse { create().thenDo { state, tokens, _ -> action(state, tokens) } }
  }

  fun orElse(action: (pipeline: Pipeline<*, *>) -> Pipeline<*, *>): Pipeline<ConditionStage, StageResponse> {
    if (lastStage !is ConditionStage) {
      throw RuntimeException("Must be in a conditional to check orElse")
    }

    val conditionStage = lastStage as ConditionStage

    val condition = {
      action(create())
    }

    val stage = ConditionStage(
      conditions = conditionStage.conditions,
      elseConditions = conditionStage.elseConditions + condition,
      getAction = conditionStage.getAction
    )

    return Pipeline(
      stages = stages.replace(lastStage, stage),
      lastStage = stage
    )
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
  private val afterActions = mutableListOf<(state: State, tokens: TokenData, response: RESPONSE_TYPE) -> Unit>()
  private val afterSuccessActions = mutableListOf<(state: State, tokens: TokenData, response: RESPONSE_TYPE) -> Unit>()

  open fun before(action: () -> Unit): BaseStage<RESPONSE_TYPE> {
    beforeActions.add(action)

    return this
  }

  open fun after(action: (state: State, tokens: TokenData, response: RESPONSE_TYPE) -> Unit): BaseStage<RESPONSE_TYPE> {
    afterActions.add(action)

    return this
  }

  open fun afterSuccess(action: (state: State, tokens: TokenData, response: RESPONSE_TYPE) -> Unit): BaseStage<RESPONSE_TYPE> {
    afterSuccessActions.add(action)

    return this
  }

  fun run(dispatcher: Dispatcher, state: State, tokens: TokenData): RESPONSE_TYPE {
    beforeActions.forEach { it() }

    val response = action(dispatcher, state, tokens)

    afterActions.forEach { it(state, tokens, response) }

    if (response.success) {
      afterSuccessActions.forEach { it(state, tokens, response) }
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
        state = response.state,
        success = acc.success && response.success
      )
    }
  },
  val getAction: (state: State, tokens: TokenData) -> List<BaseStage<*>>
) : BaseStage<StageResponse>(errorMessage, action)

data class ConditionStage(
  val conditions: List<(value: Any?) -> Pipeline<*, *>?> = emptyList(),
  val elseConditions: List<() -> Pipeline<*, *>> = emptyList(),
  override var errorMessage: String? = null,
  override val action: (dispatcher: Dispatcher, state: State, tokens: TokenData) -> StageResponse = { dispatcher, state, tokens ->
    val value = getAction(state, tokens)

    var pipelines = conditions.mapNotNull { it(value) }

    if (pipelines.isEmpty()) {
      pipelines = elseConditions.map { it() }
    }

    StageResponse(
      state = pipelines.fold(state) { acc, pipeline -> pipeline.run(dispatcher, acc, tokens) },
      success = true
    )
  },
  val getAction: (state: State, tokens: TokenData) -> Any?
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