package com.novalang.parser

import com.novalang.CompileError
import com.novalang.parser.actions.DispatcherAction
import com.novalang.replace

open class Pipeline<STAGE_TYPE : BaseStage<RESPONSE_TYPE>, RESPONSE_TYPE : BaseStageResponse> {
  protected val stages: List<BaseStage<*>>
  protected lateinit var lastStage: STAGE_TYPE

  protected constructor(
    stages: List<BaseStage<*>>
  ) {
    this.stages = stages
  }

  protected constructor(
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

  fun thenDoWithResponse(action: (response: RESPONSE_TYPE) -> Unit): Pipeline<Stage, StageResponse> {
    return thenDo { _, _, response -> action(response!!) }
  }

  fun thenDo(action: (state: State, tokens: TokenData, response: RESPONSE_TYPE?) -> Unit): Pipeline<Stage, StageResponse> {
    return thenDoStage {
      Stage { _, lastResponse, state, tokens ->
        action(state, tokens, lastResponse as RESPONSE_TYPE?)

        StageResponse(state)
      }
    }
  }

  fun thenSetState(action: (state: State) -> State): Pipeline<Stage, StageResponse> {
    val newStage = Stage { _, _, state, _ -> StageResponse(action(state)) }

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

  fun thenDoAll(action: (state: State, tokens: TokenData) -> List<BaseStage<*>>): Pipeline<ReducerStage, BaseStageResponse> {
    return thenDoStage { ReducerStage(getAction = action) }
  }

  fun orElseError(errorMessage: String): Pipeline<STAGE_TYPE, RESPONSE_TYPE> {
    return orElseError { _, _, _ -> errorMessage }
  }

  fun orElseError(action: (state: State, tokens: TokenData, response: RESPONSE_TYPE) -> String): Pipeline<STAGE_TYPE, RESPONSE_TYPE> {
    lateinit var stage: PassthroughStage<RESPONSE_TYPE>

    stage = PassthroughStage { lastResponse, state, tokens ->
      lastStage.errorMessage = action(state, tokens, lastResponse as RESPONSE_TYPE)
      // lastResponse.success = lastResponse.success// && lastStage.errorMessage == null
      // lastResponse.success = lastStage.errorMessage == null

      stage.errorMessage = lastStage.errorMessage

      lastResponse
    }

    return Pipeline(
      stages = stages + stage,
      lastStage = lastStage
    )
  }

  fun thenCheck(action: (state: State, tokens: TokenData) -> Any?): ConditionalPipeline {
    val stage = ConditionStage { state, tokens ->
      action(state, tokens)
    }

    return ConditionalPipeline(
      stages = stages + stage,
      lastStage = stage
    )
  }

  fun run(dispatcher: Dispatcher, initialState: State, tokenData: TokenData, snapshot: Boolean = true): State {
    if (snapshot) {
      tokenData.createSnapshot()
    }

    val responses = mutableListOf<BaseStageResponse>()

    val state = stages.fold(initialState) { acc, stage ->
      val lastResponse = responses.lastOrNull() ?: StageResponse(acc)
      val response = stage.run(dispatcher, lastResponse, acc, tokenData)
      responses.add(response)

      if (!response.success) {
        return@run if (stage.errorMessage != null) {
          val error = CompileError(
            message = stage.errorMessage!!,
            tokenData = tokenData.unconsumed()
          )

          initialState.copy(errors = initialState.errors + error)
        } else {
          if (snapshot) {
            tokenData.restoreSnapshot()
          }

          initialState
        }
      }

      response.state
    }

    if (snapshot) {
      tokenData.dropSnapshot()
    }

    return state
  }

  companion object {
    fun create(): Pipeline<*, *> {
      return Pipeline<BaseStage<BaseStageResponse>, BaseStageResponse>(emptyList())
    }

    @JvmStatic
    protected fun create(lastStage: BaseStage<*>): Pipeline<*, *> {
      return Pipeline<BaseStage<BaseStageResponse>, BaseStageResponse>(emptyList(), lastStage as BaseStage<BaseStageResponse>)
    }
  }
}

class ConditionalPipeline(
  stages: List<BaseStage<*>>,
  lastStage: ConditionStage
) : Pipeline<ConditionStage, StageResponse>(
  stages,
  lastStage
) {
  fun ifEqualsAnyThenDo(vararg expectedValues: Any?, action: (state: State, tokens: TokenData) -> Unit): ConditionalPipeline {
    return ifTrueThenDo({ value -> expectedValues.any { it == value } }, action)
  }

  fun ifEqualsThenDo(expectedValue: Any?, action: (state: State, tokens: TokenData) -> Unit): ConditionalPipeline {
    return ifEqualsThenDo({ expectedValue }, action)
  }

  fun ifEqualsThenDo(expectedValue: () -> Any?, action: (state: State, tokens: TokenData) -> Unit): ConditionalPipeline {
    return ifEquals(expectedValue) {
      it.thenDo { state, tokens, _ -> action(state, tokens) }
    }
  }

  fun ifEquals(expectedValue: Any?, action: (pipeline: Pipeline<*, *>) -> Pipeline<*, *>): ConditionalPipeline {
    return ifEquals({ expectedValue }, action)
  }

  fun ifEquals(expectedValue: () -> Any?, action: (pipeline: Pipeline<*, *>) -> Pipeline<*, *>): ConditionalPipeline {
    return ifTrue({ it == expectedValue() }, action)
  }

  fun ifNotEquals(expectedValue: Any?, action: (pipeline: Pipeline<*, *>) -> Pipeline<*, *>): ConditionalPipeline {
    return ifTrue({ it != expectedValue }, action)
  }

  fun ifTrueThenDo(conditionCheck: (value: Any?) -> Boolean, action: (state: State, tokens: TokenData) -> Unit): ConditionalPipeline {
    return ifTrue(conditionCheck) {
      it.thenDo { state, tokens, _ -> action(state, tokens) }
    }
  }

  fun ifTrueThenDo(action: (state: State, tokens: TokenData) -> Unit): ConditionalPipeline {
    return ifTrue {
      it.thenDo { state, tokens, _ -> action(state, tokens) }
    }
  }

  fun ifTrueThenDoAction(action: (state: State, tokens: TokenData) -> DispatcherAction): ConditionalPipeline {
    return ifTrue {
      it.thenDoAction(action)
    }
  }

  fun ifTrueThenSetState(action: (state: State) -> State): ConditionalPipeline {
    return ifTrue {
      it.thenSetState(action)
    }
  }

  fun ifTrueThenError(action: (state: State, tokens: TokenData, response: BaseStageResponse) -> String): ConditionalPipeline {
    return ifTrue {
      it.orElseError(action)
    }
  }

  fun ifTrueThenError(errorMessage: String): ConditionalPipeline {
    return ifTrue {
      it.orElseError(errorMessage)
    }
  }

  fun ifTrue(conditionCheck: (value: Any?) -> Boolean, action: (pipeline: Pipeline<*, *>) -> Pipeline<*, *>): ConditionalPipeline {
    lateinit var stage: ConditionStage

    val condition = { value: Any? ->
      if (conditionCheck(value)) {
        action(create(stage))
      } else {
        null
      }
    }

    stage = ConditionStage(
      conditions = lastStage.conditions + condition,
      elseConditions = lastStage.elseConditions,
      getAction = lastStage.getAction
    )

    return ConditionalPipeline(
      stages = stages.replace(lastStage, stage),
      lastStage = stage
    )
  }

  fun ifFalse(conditionCheck: (value: Any?) -> Boolean, action: (pipeline: Pipeline<*, *>) -> Pipeline<*, *>): ConditionalPipeline {
    lateinit var stage: ConditionStage

    val condition = { value: Any? ->
      if (!conditionCheck(value)) {
        action(create(stage))
      } else {
        null
      }
    }

    stage = ConditionStage(
      conditions = lastStage.conditions + condition,
      elseConditions = lastStage.elseConditions,
      getAction = lastStage.getAction
    )

    return ConditionalPipeline(
      stages = stages.replace(lastStage, stage),
      lastStage = stage
    )
  }

  fun ifTrue(action: (pipeline: Pipeline<*, *>) -> Pipeline<*, *>): ConditionalPipeline {
    return ifEquals(true, action)
  }

  fun ifFalse(action: (pipeline: Pipeline<*, *>) -> Pipeline<*, *>): ConditionalPipeline {
    return ifEquals(false, action)
  }

  fun ifFalseThenDo(conditionCheck: (value: Any?) -> Boolean, action: (state: State, tokens: TokenData) -> Unit): ConditionalPipeline {
    return ifFalse(conditionCheck) {
      it.thenDo { state, tokens, _ -> action(state, tokens) }
    }
  }

  fun ifFalseThenDo(action: (state: State, tokens: TokenData) -> Unit): ConditionalPipeline {
    return ifFalse {
      it.thenDo { state, tokens, _ -> action(state, tokens) }
    }
  }

  fun ifFalseThenDoAction(action: (state: State, tokens: TokenData) -> DispatcherAction): ConditionalPipeline {
    return ifFalse {
      it.thenDoAction(action)
    }
  }

  fun ifFalseThenSetState(action: (state: State) -> State): ConditionalPipeline {
    return ifFalse {
      it.thenSetState(action)
    }
  }

  fun orElseExit(): ConditionalPipeline {
    return orElse {
      create(lastStage)
        .thenDo { state, _, _ ->
          StageResponse(
            state = state,
            success = false
          )
        }
    }
  }

  fun orElseThenDo(action: (state: State, tokens: TokenData) -> Unit): ConditionalPipeline {
    return orElse { create(lastStage).thenDo { state, tokens, _ -> action(state, tokens) } }
  }

  fun orElse(action: (pipeline: Pipeline<*, *>) -> Pipeline<*, *>): ConditionalPipeline {
    lateinit var stage: ConditionStage

    val condition = {
      action(create(stage))
    }

    stage = ConditionStage(
      conditions = lastStage.conditions,
      elseConditions = lastStage.elseConditions + condition,
      getAction = lastStage.getAction
    )

    return ConditionalPipeline(
      stages = stages.replace(lastStage, stage),
      lastStage = stage
    )
  }
}

abstract class BaseStage<RESPONSE_TYPE : BaseStageResponse>(
  open var errorMessage: String? = null,
  open val action: (dispatcher: Dispatcher, lastResponse: BaseStageResponse, state: State, tokens: TokenData) -> RESPONSE_TYPE
) {
  fun run(dispatcher: Dispatcher, lastResponse: BaseStageResponse, state: State, tokens: TokenData): RESPONSE_TYPE {
    return action(dispatcher, lastResponse, state, tokens)
  }
}

data class Stage(
  override var errorMessage: String? = null,
  override val action: (dispatcher: Dispatcher, lastResponse: BaseStageResponse, state: State, tokens: TokenData) -> StageResponse
) : BaseStage<StageResponse>(errorMessage, action)

data class PassthroughStage<RESPONSE_TYPE : BaseStageResponse>(
  override var errorMessage: String? = null,
  override val action: (dispatcher: Dispatcher, lastResponse: BaseStageResponse, state: State, tokens: TokenData) -> RESPONSE_TYPE = { _, lastResponse, state, tokens ->
    getAction(lastResponse, state, tokens)
  },
  val getAction: (lastResponse: BaseStageResponse, state: State, tokens: TokenData) -> RESPONSE_TYPE
) : BaseStage<RESPONSE_TYPE>(errorMessage, action)

data class ReducerStage(
  override var errorMessage: String? = null,
  override val action: (dispatcher: Dispatcher, lastResponse: BaseStageResponse, state: State, tokens: TokenData) -> BaseStageResponse = { dispatcher, lastResponse, state, tokens ->
    getAction(state, tokens).fold(lastResponse) { acc, stage ->
      val response = stage.action(dispatcher, acc, acc.state, tokens)

      StageResponse(
        state = response.state,
        success = acc.success && response.success
      )
    }
  },
  val getAction: (state: State, tokens: TokenData) -> List<BaseStage<*>>
) : BaseStage<BaseStageResponse>(errorMessage, action)

data class ConditionStage(
  val conditions: List<(value: Any?) -> Pipeline<*, *>?> = emptyList(),
  val elseConditions: List<() -> Pipeline<*, *>> = emptyList(),
  override var errorMessage: String? = null,
  override val action: (dispatcher: Dispatcher, lastResponse: BaseStageResponse, state: State, tokens: TokenData) -> StageResponse = { dispatcher, _, state, tokens ->
    val value = getAction(state, tokens)

    var pipelines = conditions.mapNotNull { it(value) }

    if (pipelines.isEmpty()) {
      pipelines = elseConditions.map { it() }
    }

    StageResponse(
      state = pipelines.fold(state) { acc, pipeline -> pipeline.run(dispatcher, acc, tokens, false) },
      success = true
    )
  },
  val getAction: (state: State, tokens: TokenData) -> Any?
) : BaseStage<StageResponse>(errorMessage, action)

data class ExpectTokenStage(
  override var errorMessage: String? = null,
  override val action: (dispatcher: Dispatcher, lastResponse: BaseStageResponse, state: State, tokens: TokenData) -> ExpectTokenStageResponse = { _, _, state, tokens ->
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
  override val action: (dispatcher: Dispatcher, lastResponse: BaseStageResponse, state: State, tokens: TokenData) -> ExpectTokenCountStageResponse = { _, _, state, (tokens) ->
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
  override val action: (dispatcher: Dispatcher, lastResponse: BaseStageResponse, state: State, tokens: TokenData) -> StageResponse = { _, _, state, tokens ->
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
  override val action: (dispatcher: Dispatcher, lastResponse: BaseStageResponse, state: State, tokens: TokenData) -> StageResponse = { dispatcher, _, state, tokens ->
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
  open var success: Boolean = true
) {
}

data class StageResponse(
  override val state: State,
  override var success: Boolean = true
) : BaseStageResponse(
  state,
  success
)

data class ExpectTokenStageResponse(
  val token: Token?,
  override val state: State,
  override var success: Boolean = true
) : BaseStageResponse(
  state,
  success
)

data class ExpectTokenCountStageResponse(
  val expected: Int,
  val actual: Int,
  override val state: State,
  override var success: Boolean = true
) : BaseStageResponse(
  state,
  success
)