package com.novalang.parser.ast

import com.novalang.ast.Function
import com.novalang.parser.ActionStage
import com.novalang.parser.Dispatcher
import com.novalang.parser.Pipeline
import com.novalang.parser.State
import com.novalang.parser.Token
import com.novalang.parser.TokenData
import com.novalang.parser.TokenList
import com.novalang.parser.TokenType
import com.novalang.parser.actions.AddFunctionAction
import com.novalang.parser.actions.AddParameterAction
import com.novalang.parser.actions.ClassParseAction
import com.novalang.parser.actions.DispatcherAction
import com.novalang.parser.actions.EndScopeAction
import com.novalang.parser.actions.ParameterParseAction
import com.novalang.parser.actions.ReplaceFunctionAction
import com.novalang.parser.actions.ReplaceScopeAction
import com.novalang.parser.actions.StartScopeAction

class FunctionParser(dispatcher: Dispatcher) : Reducer(dispatcher) {
  override fun reduce(state: State, action: DispatcherAction): State {
    return when (action) {
      is ClassParseAction -> parseFunction(state, action.tokenData)
      is AddParameterAction -> addParameter(action).run(dispatcher, state, action.tokenData)
      is ReplaceScopeAction -> replaceScope(state, action)
      is EndScopeAction -> endScope(state, action)
      else -> state
    }
  }

  private fun endScope(state: State, action: EndScopeAction): State {
    if (state.currentFunction?.scope == action.scope) {
      return state.copy(
        currentFunction = null
      )
    }

    return state
  }

  private fun replaceScope(state: State, action: ReplaceScopeAction): State {
    if (state.currentFunction?.scope == action.oldScope) {
      val newFunction = state.currentFunction.copy(
        scope = action.newScope
      )

      return dispatcher.dispatchAndExecute(
        state,
        ReplaceFunctionAction(
          oldFunction = state.currentFunction,
          newFunction = newFunction
        )
      )
    }

    return state
  }

  private fun addParameter(action: AddParameterAction): Pipeline<*, *> {
    return Pipeline.create()
      .thenDoAction { state, _ ->
        val newFunction = state.currentFunction!!.copy(
          parameters = state.currentFunction.parameters + action.parameter
        )

        ReplaceFunctionAction(
          oldFunction = state.currentFunction,
          newFunction = newFunction
        )
      }
  }

  private fun parseFunction(initialState: State, tokenData: TokenData): State {
    lateinit var nameToken: Token

    return Pipeline.create()
      .thenExpectToken { (tokens) -> tokens.consumeFirstIfType(TokenType.IDENTIFIER) }
      .thenDo { nameToken = it.token!! }

      .thenExpectToken { (tokens) -> tokens.consumeFirstIfType(TokenType.OPENING_PAREN) }

      .thenExpectToken { (tokens) -> tokens.consumeAtReverseIndexIfType(1, TokenType.CLOSING_PAREN) }
      .orElseError { "Function missing ending parenthesis" }

      .thenExpectToken { (tokens) -> tokens.consumeAtReverseIndexIfType(0, TokenType.OPENING_BRACE) }
      .orElseError { "Function missing declaration scope" }

      .thenDoAction { _, _ -> AddFunctionAction(Function(nameToken.value)) }

      .thenDoAll { _, (tokens) ->
        val parameterTokens = mutableListOf(
          mutableListOf<Token>()
        )

        while (tokens.isNotConsumed()) {
          if (tokens.unconsumed.first().type == TokenType.COMMA) {
            tokens.consumeFirst()

            parameterTokens.add(mutableListOf())
          } else {
            parameterTokens.last().add(tokens.consumeFirst())
          }
        }

        parameterTokens
          .filter { it.isNotEmpty() }
          .map {
            ActionStage { state, _ ->
              val parameterTokenData = TokenData(
                tokens = TokenList(it),
                source = tokenData.source
              )

              ParameterParseAction(
                tokenData = parameterTokenData,
                function = state.currentFunction!!
              )
            }
          }
      }

      .thenDoAction { _, tokens -> StartScopeAction(tokens) }

      .run(dispatcher, initialState, tokenData)
  }
}
