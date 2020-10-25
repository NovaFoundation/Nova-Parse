package com.novalang.parser.ast

import com.novalang.CompileError
import com.novalang.ast.Assignment
import com.novalang.ast.IfStatement
import com.novalang.ast.Literal
import com.novalang.ast.Variable
import com.novalang.parser.Dispatcher
import com.novalang.parser.State
import com.novalang.parser.TokenData
import com.novalang.parser.TokenType
import com.novalang.parser.actions.AddIfStatementAction
import com.novalang.parser.actions.AddIfStatementValueAction
import com.novalang.parser.actions.AssignmentValueParseAction
import com.novalang.parser.actions.DispatcherAction
import com.novalang.parser.actions.IfStatementValueParseAction
import com.novalang.parser.actions.ScopeParseAction

class IfStatementParser(dispatcher: Dispatcher) : Reducer(dispatcher) {
  override fun reduce(state: State, action: DispatcherAction): State {
    return when (action) {
      is ScopeParseAction -> parseIfStatement(state, action)
      is AddIfStatementValueAction -> addIfStatementValue(state, action)
      else -> state
    }
  }

  private fun addIfStatementValue(state: State, action: AddIfStatementValueAction): State {
    return dispatcher.dispatchAndExecute(
      state.copy(
        currentIfStatement = null
      ),
      AddIfStatementAction(
        file = state.currentFile!!,
        clazz = state.currentClass!!,
        ifStatement = state.currentIfStatement!!.copy(expression = action.value)
      )
    )
  }

  private fun error(state: State, tokenData: TokenData, message: String): State {
    return state.copy(
      errors = state.errors + CompileError(
        message = message,
        tokenData = tokenData.unconsumed()
      )
    )
  }

  private fun parseIfStatement(state: State, action: ScopeParseAction): State {
    val tokens = action.tokenData.currentTokens

    tokens.consumeFirstIfType(TokenType.IF) ?: return state
    tokens.consumeFirstIfType(TokenType.OPENING_PAREN) ?: return error(state, action.tokenData, "If statement missing opening parenthesis")
    tokens.consumeLastIfType(TokenType.OPENING_BRACE) ?: return error(state, action.tokenData, "If statement missing opening brace")
    tokens.consumeLastIfType(TokenType.CLOSING_PAREN) ?: return error(state, action.tokenData, "If statement missing closing parenthesis")

    if (tokens.isConsumed()) {
      tokens.unconsumeLast()

      return error(state, action.tokenData, "If statement missing expression")
    }

    val ifStatement = IfStatement(
      expression = Literal.NULL
    )

    return dispatcher.dispatchAndExecute(
      state = state.copy(currentIfStatement = ifStatement),
      action = IfStatementValueParseAction(tokenData = action.tokenData)
    )
  }
}
