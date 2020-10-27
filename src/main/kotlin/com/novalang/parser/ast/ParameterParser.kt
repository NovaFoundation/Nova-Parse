package com.novalang.parser.ast

import com.novalang.ast.Parameter
import com.novalang.parser.Dispatcher
import com.novalang.parser.Pipeline
import com.novalang.parser.State
import com.novalang.parser.Token
import com.novalang.parser.TokenType
import com.novalang.parser.actions.AddParameterAction
import com.novalang.parser.actions.DispatcherAction
import com.novalang.parser.actions.ParameterParseAction

class ParameterParser(dispatcher: Dispatcher) : Reducer(dispatcher) {
  override fun reduce(state: State, action: DispatcherAction): State {
    return when (action) {
      is ParameterParseAction -> parseParameter(state, action)
      else -> state
    }
  }

  private fun parseParameter(state: State, action: ParameterParseAction): State {
    return when (action.tokenData.tokens.unconsumed[0].type) {
      TokenType.LET -> parseConstant(state, action)
      TokenType.VAR -> parseVariable(state, action)
      else -> parseConstant(state, action)
    }
  }

  private fun parseConstant(state: State, action: ParameterParseAction): State {
    val currentTokens = action.tokenData.tokens

    if (currentTokens.unconsumed.first().type == TokenType.LET) {
      currentTokens.consumeFirst()
    }

    return parseParameterData(true).run(dispatcher, state, action.tokenData)
  }

  private fun parseVariable(state: State, action: ParameterParseAction): State {
    val currentTokens = action.tokenData.tokens

    if (currentTokens.unconsumed.first().type == TokenType.VAR) {
      currentTokens.consumeFirst()
    }

    return parseParameterData(false).run(dispatcher, state, action.tokenData)
  }

  private fun parseParameterData(constant: Boolean): Pipeline<*, *> {
    lateinit var nameToken: Token
    lateinit var typeToken: Token

    return Pipeline.create()
      .thenExpectToken { (tokens) -> tokens.consumeFirstIfType(TokenType.IDENTIFIER) }
      .orElseError { "Missing parameter name" }
      .thenDo { nameToken = it.token!! }

      .thenCheck { _, (tokens) -> tokens.isConsumed() }

      .ifTrue {
        it.thenDoAction { _, _ ->
          AddParameterAction(
            parameter = Parameter(
              name = nameToken.value,
              type = null,
              constant = constant
            )
          )
        }
      }

      .ifFalse {
        it.thenExpectToken { (tokens) -> tokens.consumeFirstIfType(TokenType.COLON) }
          .orElseError { "Invalid parameter type declaration" }

          .thenExpectToken { (tokens) -> tokens.consumeFirstIfType(TokenType.IDENTIFIER) }
          .thenDo { typeToken = it.token!! }
          .orElseError { "Invalid parameter type" }

          .thenDoAction { _, _ ->
            AddParameterAction(
              Parameter(
                name = nameToken.value,
                type = typeToken.value,
                constant = constant
              )
            )
          }
      }
  }
}
