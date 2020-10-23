package com.novalang.parser.ast

import com.novalang.CompileError
import com.novalang.ast.Function
import com.novalang.parser.Dispatcher
import com.novalang.parser.State
import com.novalang.parser.Token
import com.novalang.parser.TokenData
import com.novalang.parser.TokenType
import com.novalang.parser.actions.AddFunctionAction
import com.novalang.parser.actions.ClassParseAction
import com.novalang.parser.actions.DispatcherAction

class FunctionParser(private val dispatcher: Dispatcher) : Reducer() {
  override fun reduce(state: State, action: DispatcherAction): State {
    return when (action) {
      is ClassParseAction -> parseClass(state, action.tokenData)
      else -> state
    }
  }

  private fun parseClass(state: State, tokenData: TokenData): State {
    if (
      tokenData.currentTokens.unconsumed.size > 2 &&
      tokenData.currentTokens.unconsumed[1].type == TokenType.OPENING_PAREN
    ) {
      if (tokenData.currentTokens.unconsumed.last().type != TokenType.OPENING_BRACE) {
        tokenData.currentTokens.consumeAll()

        return state.copy(
          errors = state.errors + CompileError(
            message = "Missing function declaration scope",
            source = tokenData.source
          )
        )
      }

      val name = tokenData.currentTokens.consumeFirst().value

      // opening paren
      tokenData.currentTokens.consumeFirst()

      val tokens = mutableListOf(
        mutableListOf<Token>()
      )

      while (tokenData.currentTokens.isNotConsumed() && tokenData.currentTokens.unconsumed.first().type != TokenType.CLOSING_PAREN) {
        if (tokenData.currentTokens.unconsumed.first().type == TokenType.COMMA) {
          tokenData.currentTokens.consumeFirst()

          tokens.add(mutableListOf())
        } else {
          tokens.last().add(tokenData.currentTokens.consumeFirst())
        }
      }

      // closing paren
      tokenData.currentTokens.consumeFirst()

      if (tokenData.currentTokens.unconsumed.size > 1) {
        tokenData.currentTokens.consumeAllButLast()

        return state.copy(
          errors = state.errors + CompileError(
            message = "Invalid function declaration",
            source = tokenData.source
          )
        )
      }

      val function = Function(name = name)

      dispatcher.dispatch(
        AddFunctionAction(
          file = state.currentFile!!,
          clazz = state.currentClass!!,
          function = function
        )
      )

      return state
    }

    return state
  }
}
