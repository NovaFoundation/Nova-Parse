package com.novalang.parser.ast

import com.novalang.ast.IfStatement
import com.novalang.ast.Literal
import com.novalang.parser.Dispatcher
import com.novalang.parser.State
import com.novalang.parser.TokenData
import com.novalang.parser.TokenType
import com.novalang.parser.actions.AddElseStatementIfStatementAction
import com.novalang.parser.actions.AddIfStatementAction
import com.novalang.parser.actions.AddIfStatementValueAction
import com.novalang.parser.actions.DispatcherAction
import com.novalang.parser.actions.ElseStatementIfStatementParseAction
import com.novalang.parser.actions.EndScopeAction
import com.novalang.parser.actions.IfStatementValueParseAction
import com.novalang.parser.actions.ReplaceStatementAction
import com.novalang.parser.actions.ReplaceScopeAction
import com.novalang.parser.actions.ScopeParseAction

class IfStatementParser(dispatcher: Dispatcher) : Reducer(dispatcher) {
  override fun reduce(state: State, action: DispatcherAction): State {
    return when (action) {
      is ScopeParseAction -> parseIfStatement(state, action.tokenData)
      is ReplaceScopeAction -> replaceScope(state, action)
      is ElseStatementIfStatementParseAction -> parseIfStatementForElseStatement(state, action)
      is AddIfStatementValueAction -> addIfStatementValue(state, action)
      is AddIfStatementAction -> addIfStatement(state, action)
      is EndScopeAction -> endScope(state, action)
      else -> state
    }
  }

  private fun endScope(state: State, action: EndScopeAction): State {
    if (state.currentIfStatement?.scope == action.scope) {
      val newIfStatements = state.ifStatements - state.currentIfStatement

      return state.copy(
        currentIfStatement = newIfStatements.lastOrNull(),
        ifStatements = newIfStatements
      )
    }

    return state
  }

  private fun addIfStatement(state: State, action: AddIfStatementAction): State {
    return state.copy(
      currentIfStatement = action.ifStatement,
      ifStatements = state.ifStatements + action.ifStatement
    )
  }

  private fun replaceScope(state: State, action: ReplaceScopeAction): State {
    val ifStatement = state.ifStatements.find { it.scope == action.oldScope }

    if (ifStatement != null) {
      return dispatcher.dispatchAndExecute(
        state = state,
        action = ReplaceStatementAction(
          oldStatement = ifStatement,
          newStatement = ifStatement.copy(scope = action.newScope)
        )
      )
    }

    return state
  }

  private fun parseIfStatementForElseStatement(state: State, action: ElseStatementIfStatementParseAction): State {
    return parseIfStatement(state, action.tokenData)
  }

  private fun addIfStatementValue(state: State, action: AddIfStatementValueAction): State {
    return if (state.currentElseStatement != null) {
      dispatcher.dispatchAndExecute(
        state,
        AddElseStatementIfStatementAction(
          file = state.currentFile!!,
          clazz = state.currentClass!!,
          ifStatement = state.currentIfStatement!!.copy(expression = action.value)
        )
      )
    } else {
      dispatcher.dispatchAndExecute(
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
  }

  private fun parseIfStatement(state: State, tokenData: TokenData): State {
    val tokens = tokenData.currentTokens

    tokens.consumeFirstIfType(TokenType.IF) ?: return state
    tokens.consumeFirstIfType(TokenType.OPENING_PAREN) ?: return error(state, tokenData, "If statement missing opening parenthesis")
    tokens.consumeLastIfType(TokenType.OPENING_BRACE) ?: return error(state, tokenData, "If statement missing opening brace")
    tokens.consumeLastIfType(TokenType.CLOSING_PAREN) ?: return error(state, tokenData, "If statement missing closing parenthesis")

    if (tokens.isConsumed()) {
      tokens.unconsumeLast() // still allow parsing the scope as a scope block

      return error(state, tokenData, "If statement missing expression")
    }

    val ifStatement = IfStatement(
      expression = Literal.NULL
    )

    return dispatcher.dispatchAndExecute(
      state = state.copy(currentIfStatement = ifStatement),
      action = IfStatementValueParseAction(tokenData = tokenData)
    )
  }
}
