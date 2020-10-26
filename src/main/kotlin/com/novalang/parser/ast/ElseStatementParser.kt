package com.novalang.parser.ast

import com.novalang.ast.ElseStatement
import com.novalang.parser.Dispatcher
import com.novalang.parser.State
import com.novalang.parser.TokenType
import com.novalang.parser.actions.AddElseStatementAction
import com.novalang.parser.actions.AddElseStatementIfStatementAction
import com.novalang.parser.actions.DispatcherAction
import com.novalang.parser.actions.ElseStatementIfStatementParseAction
import com.novalang.parser.actions.ScopeParseAction

class ElseStatementParser(dispatcher: Dispatcher) : Reducer(dispatcher) {
  override fun reduce(state: State, action: DispatcherAction): State {
    return when (action) {
      is ScopeParseAction -> parseElseStatement(state, action)
      is AddElseStatementIfStatementAction -> addElseStatementValue(state, action)
      else -> state
    }
  }

  private fun addElseStatementValue(state: State, action: AddElseStatementIfStatementAction): State {
    return dispatcher.dispatchAndExecute(
      state.copy(
        currentElseStatement = null
      ),
      AddElseStatementAction(
        file = state.currentFile!!,
        clazz = state.currentClass!!,
        elseStatement = state.currentElseStatement!!.copy(ifStatement = action.ifStatement)
      )
    )
  }

  private fun parseElseStatement(state: State, action: ScopeParseAction): State {
    val tokens = action.tokenData.tokens

    tokens.consumeFirstIfType(TokenType.ELSE) ?: return state

    if (tokens.isConsumed() || tokens.unconsumed.last().type != TokenType.OPENING_BRACE) {
      return error(state, action.tokenData, "Else statement missing opening brace")
    } else if (tokens.isConsumed()) {
      val elseStatement = ElseStatement()

      return dispatcher.dispatchAndExecute(
        state = state,
        action = AddElseStatementAction(
          file = state.currentFile!!,
          clazz = state.currentClass!!,
          elseStatement = elseStatement
        )
      )
    }

    val elseStatement = ElseStatement()

    return dispatcher.dispatchAndExecute(
      state = state.copy(currentElseStatement = elseStatement),
      action = ElseStatementIfStatementParseAction(tokenData = action.tokenData)
    )
  }
}
