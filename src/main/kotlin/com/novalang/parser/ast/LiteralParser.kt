package com.novalang.parser.ast

import com.novalang.ast.Literal
import com.novalang.ast.LiteralType
import com.novalang.parser.Dispatcher
import com.novalang.parser.Pipeline
import com.novalang.parser.State
import com.novalang.parser.TokenData
import com.novalang.parser.TokenType
import com.novalang.parser.actions.AddAssignmentValueAction
import com.novalang.parser.actions.AddIfStatementValueAction
import com.novalang.parser.actions.AssignmentValueParseAction
import com.novalang.parser.actions.DispatcherAction
import com.novalang.parser.actions.IfStatementValueParseAction

private val NUMBERS = setOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')

private const val BOOLEAN_TRUE_VALUE = "true"
private const val BOOLEAN_FALSE_VALUE = "false"

class LiteralParser(dispatcher: Dispatcher) : Reducer(dispatcher) {
  override fun reduce(state: State, action: DispatcherAction): State {
    return when (action) {
      is AssignmentValueParseAction -> parseLiteralForAssignment(state, action).run(dispatcher, state, action.tokenData)
      is IfStatementValueParseAction -> parseLiteralForIfStatement(state, action).run(dispatcher, state, action.tokenData)
      else -> state
    }
  }

  private fun parseLiteralForAssignment(state: State, action: AssignmentValueParseAction): Pipeline<*, *> {
    val literal = parseLiteral(action.tokenData)

    return Pipeline.create()
      .thenCheck { _, _ -> literal }
      .ifNotEquals(null) { it, _ ->
        it.thenDoAction { _, _ ->
          AddAssignmentValueAction(
            file = state.currentFile!!,
            clazz = state.currentClass!!,
            value = literal!!
          )
        }
      }
  }

  private fun parseLiteralForIfStatement(state: State, action: IfStatementValueParseAction): Pipeline<*, *> {
    val literal = parseLiteral(action.tokenData)

    return Pipeline.create()
      .thenCheck { _, _ -> literal }
      .ifNotEquals(null) { it, _ ->
        it.thenDoAction { _, _ ->
          AddIfStatementValueAction(
            file = state.currentFile!!,
            clazz = state.currentClass!!,
            value = literal!!
          )
        }
      }
  }

  private fun parseLiteral(tokenData: TokenData): Literal? {
    val tokens = tokenData.tokens

    if (tokens.unconsumed.size == 1) {
      val token = tokens.unconsumed.first()

      if (token.type == TokenType.IDENTIFIER) {
        val literal = when {
          token.value == BOOLEAN_TRUE_VALUE -> Literal(token.value, LiteralType.BOOLEAN)
          token.value == BOOLEAN_FALSE_VALUE -> Literal(token.value, LiteralType.BOOLEAN)
          token.value.all { NUMBERS.contains(it) } -> Literal(token.value, getNumberLiteralType(token.value))
          else -> null
        }

        if (literal != null) {
          tokens.consumeFirst()

          return literal
        }
      }
    }

    return null
  }

  private fun getNumberLiteralType(value: String): LiteralType {
    return when (val number = value.toLong()) {
      number.toByte().toLong() -> LiteralType.BYTE
      number.toShort().toLong() -> LiteralType.SHORT
      number.toInt().toLong() -> LiteralType.INT
      else -> LiteralType.LONG
    }
  }
}
