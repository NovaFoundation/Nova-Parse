package com.novalang.parser

import com.novalang.CompileError
import com.novalang.parser.actions.DispatcherAction
import com.novalang.parser.actions.ParseAction
import com.novalang.parser.ast.Reducer

class Dispatcher {
  private var currentData: Any? = null
  private var reducers: List<Reducer> = emptyList()
  private val pendingActions: MutableList<DispatcherAction> = mutableListOf()

  fun dispatch(action: DispatcherAction) {
    pendingActions.add(action)
  }

  fun dispatchAndExecuteAll(state: State, action: DispatcherAction): State {
    pendingActions.add(action)

    return nextAll(state)
  }

  fun dispatchAndExecute(state: State, action: DispatcherAction): State {
    pendingActions.add(0, action)

    return next(state)
  }

  // fun waitFor(type: Class<DispatcherAction>) {
  // }

  fun nextAll(state: State): State {
    var currentState = state

    while (hasNext()) {
      currentState = next(currentState)
    }

    return currentState
  }

  fun next(initialState: State): State {
    var state = initialState
    val action = pendingActions.removeFirst()

    state = reducers.fold(state) { acc, reducer ->
      if (action !is ParseAction || (action.tokenData.currentTokens.tokens.isEmpty() || action.tokenData.currentTokens.isNotConsumed())) {
        reducer.reduce(acc, action)
      } else {
        acc
      }
    }

    return if (action is ParseAction && action.tokenData.currentTokens.isNotConsumed()) {
      state.copy(
        errors = state.errors + CompileError(
          message = "Unconsumed statement",
          tokenData = action.tokenData
        )
      )
    } else {
      state
    }
  }

  fun hasNext(): Boolean {
    return pendingActions.isNotEmpty()
  }

  fun register(reducer: Reducer): Dispatcher {
    reducers = reducers + reducer

    return this
  }
}

