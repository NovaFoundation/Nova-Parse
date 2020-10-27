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
      is ParameterParseAction -> parseParameter().run(dispatcher, state, action.tokenData)
      else -> state
    }
  }

  private fun parseParameter(): Pipeline<*, *> {
    var constant = false
    lateinit var nameToken: Token
    lateinit var typeToken: Token

    return Pipeline.create()
      .thenCheck { _, (tokens) -> tokens.unconsumed.first().type }
      .ifEqualsAnyThenDo(TokenType.VAR, TokenType.LET) { _, tokens -> tokens.consumeFirst() }
      .ifEqualsThenDo(TokenType.VAR) { _, _ -> constant = false }
      .ifEqualsThenDo(TokenType.LET) { _, _ -> constant = true }
      .orElseThenDo { _, _ -> constant = true }

      .thenExpectToken { (tokens) -> tokens.consumeFirstIfType(TokenType.IDENTIFIER) }
      .orElseError("Missing parameter name")
      .thenDoWithResponse { nameToken = it.token!! }

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
          .orElseError("Invalid parameter type declaration")

          .thenExpectToken { (tokens) -> tokens.consumeFirstIfType(TokenType.IDENTIFIER) }
          .thenDoWithResponse { typeToken = it.token!! }
          .orElseError("Invalid parameter type")

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
