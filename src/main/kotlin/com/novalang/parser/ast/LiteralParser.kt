package com.novalang.parser.ast

import com.novalang.ast.Literal
import com.novalang.parser.Dispatcher
import com.novalang.parser.State
import com.novalang.parser.TokenData
import com.novalang.parser.TokenType
import com.novalang.parser.actions.AddAssignmentValueAction
import com.novalang.parser.actions.AssignmentValueParseAction
import com.novalang.parser.actions.DispatcherAction

private val NUMBERS = setOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')

class LiteralParser(dispatcher: Dispatcher) : Reducer(dispatcher) {
  override fun reduce(state: State, action: DispatcherAction): State {
    return when (action) {
      is AssignmentValueParseAction -> parseLiteralForAssignment(state, action)
      else -> state
    }
  }

  private fun parseLiteralForAssignment(state: State, action: AssignmentValueParseAction): State {
    val literal = parseLiteral(action.tokenData)

    if (literal != null) {
      return dispatcher.dispatchAndExecute(
        state,
        AddAssignmentValueAction(
          file = state.currentFile!!,
          clazz = state.currentClass!!,
          value = literal
        )
      )
    }

    return state
  }

  private fun parseLiteral(tokenData: TokenData): Literal? {
    val tokens = tokenData.currentTokens

    if (tokens.unconsumed.size == 1) {
      val token = tokens.unconsumed.first()

      if (token.type == TokenType.IDENTIFIER) {
        if (token.value.asSequence().all { NUMBERS.contains(it) }) {
          tokens.consumeAll()

          return Literal(token.value)
        }
      }
    }

    return null
  }
}
