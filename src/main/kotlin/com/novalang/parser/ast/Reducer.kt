package com.novalang.parser.ast

import com.novalang.CompileError
import com.novalang.parser.Dispatcher
import com.novalang.parser.State
import com.novalang.parser.TokenData
import com.novalang.parser.actions.DispatcherAction

abstract class Reducer(protected val dispatcher: Dispatcher) {
  abstract fun reduce(state: State, action: DispatcherAction): State

  protected fun restoreSnapshot(state: State, tokenData: TokenData): State {
    tokenData.restoreSnapshot()

    return state
  }

  protected fun error(state: State, tokenData: TokenData, message: String): State {
    return state.copy(
      errors = state.errors + CompileError(
        message = message,
        tokenData = tokenData.unconsumed()
      )
    )
  }
}
