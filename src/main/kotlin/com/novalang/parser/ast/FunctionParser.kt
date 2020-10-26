package com.novalang.parser.ast

import com.novalang.CompileError
import com.novalang.ast.Function
import com.novalang.parser.Dispatcher
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
      is AddParameterAction -> addParameter(state, action)
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
          file = state.currentFile!!,
          clazz = state.currentClass!!,
          oldFunction = state.currentFunction,
          newFunction = newFunction
        )
      )
    }

    return state
  }

  private fun addParameter(state: State, action: AddParameterAction): State {
    val newFunction = action.function.copy(
      parameters = action.function.parameters + action.parameter
    )

    return dispatcher.dispatchAndExecute(
      state,
      ReplaceFunctionAction(
        file = action.file,
        clazz = action.clazz,
        oldFunction = action.function,
        newFunction = newFunction
      )
    )
  }

  private fun parseFunction(initialState: State, tokenData: TokenData): State {
    var state = initialState
    val tokens = tokenData.currentTokens

    tokenData.createSnapshot()

    val nameToken = tokens.consumeFirstIfType(TokenType.IDENTIFIER) ?: return restoreSnapshot(state, tokenData)
    tokens.consumeFirstIfType(TokenType.OPENING_PAREN) ?: return restoreSnapshot(state, tokenData)
    tokens.consumeAtReverseIndexIfType(1, TokenType.CLOSING_PAREN) ?: return error(state, tokenData, "Function missing ending parenthesis")
    tokens.consumeAtReverseIndexIfType(0, TokenType.OPENING_BRACE) ?: return error(state, tokenData, "Function missing declaration scope")

    val function = Function(name = nameToken.value)

    state = dispatcher.dispatchAndExecute(
      state,
      AddFunctionAction(
        file = state.currentFile!!,
        clazz = state.currentClass!!,
        function = function
      )
    )

    if (tokens.unconsumed.isNotEmpty()) {
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

      state = parameterTokens
        .filter { it.isNotEmpty() }
        .fold(state) { acc, tokens ->
          val parameterTokenData = TokenData(
            currentTokens = TokenList(tokens),
            source = tokenData.source
          )

          dispatcher.dispatchAndExecute(
            acc,
            ParameterParseAction(
              tokenData = parameterTokenData,
              function = acc.currentFunction!!
            )
          )
        }
    }

    state = dispatcher.dispatchAndExecute(
      state,
      StartScopeAction(
        tokenData = tokenData
      )
    )

    return state
  }
}
