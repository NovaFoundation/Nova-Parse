package com.novalang.parser.ast

import com.novalang.parser.Dispatcher
import com.novalang.parser.State
import com.novalang.parser.actions.DispatcherAction

abstract class Reducer(protected val dispatcher: Dispatcher) {
  abstract fun reduce(state: State, action: DispatcherAction): State
}
