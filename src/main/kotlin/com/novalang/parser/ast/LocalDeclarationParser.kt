package com.novalang.parser.ast

import com.novalang.ast.LocalDeclaration
import com.novalang.parser.Dispatcher
import com.novalang.parser.Pipeline
import com.novalang.parser.State
import com.novalang.parser.Token
import com.novalang.parser.TokenType
import com.novalang.parser.actions.AddLocalDeclarationAction
import com.novalang.parser.actions.DispatcherAction
import com.novalang.parser.actions.ScopeParseAction

class LocalDeclarationParser(dispatcher: Dispatcher) : Reducer(dispatcher) {
  override fun reduce(state: State, action: DispatcherAction): State {
    return when (action) {
      is ScopeParseAction -> parseLocalDeclaration().run(dispatcher, state, action.tokenData)
      else -> state
    }
  }

  private fun parseLocalDeclaration(): Pipeline<*, *> {
    var constant = false
    lateinit var nameToken: Token
    lateinit var typeToken: Token

    return Pipeline.create()
      .thenCheck { _, (tokens) -> tokens.unconsumed.first().type }
      .ifEqualsAnyThenDo(TokenType.VAR, TokenType.LET) { _, (tokens) -> tokens.consumeFirst() }
      .ifEqualsThenDo(TokenType.VAR) { _, _ -> constant = false }
      .ifEqualsThenDo(TokenType.LET) { _, _ -> constant = true }
      .orElseThenDo { _, _ -> constant = true }

      .thenExpectToken { (tokens) -> tokens.consumeFirstIfType(TokenType.IDENTIFIER) }
      .orElseError("Missing variable declaration name")
      .thenDoWithResponse { nameToken = it.token!! }

      .thenCheck { _, (tokens) -> tokens.isConsumed() }

      .ifTrue { it, _ ->
        it.thenDoAction { _, _ ->
          AddLocalDeclarationAction(
            LocalDeclaration(
              name = nameToken.value,
              type = null,
              constant = constant
            )
          )
        }
      }

      .ifFalse { it, _ ->
        it.thenExpectToken { (tokens) -> tokens.consumeFirstIfType(TokenType.COLON) }
          .orElseError("Invalid variable type declaration")

          .thenExpectToken { (tokens) -> tokens.consumeFirstIfType(TokenType.IDENTIFIER) }
          .thenDoWithResponse { typeToken = it.token!! }
          .orElseError("Invalid variable declaration type")

          .thenDoAction { _, _ ->
            AddLocalDeclarationAction(
              LocalDeclaration(
                name = nameToken.value,
                type = typeToken.value,
                constant = constant
              )
            )
          }
      }
  }
}
