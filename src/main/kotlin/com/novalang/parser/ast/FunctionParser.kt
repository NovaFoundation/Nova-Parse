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
import com.novalang.parser.actions.ParameterParseAction
import com.novalang.parser.actions.ReplaceFunctionAction
import com.novalang.parser.actions.ReplaceScopeAction

class FunctionParser(dispatcher: Dispatcher) : Reducer(dispatcher) {
  override fun reduce(state: State, action: DispatcherAction): State {
    return when (action) {
      is ClassParseAction -> parseClass(state, action.tokenData)
      is AddParameterAction -> addParameter(state, action)
      is ReplaceScopeAction -> replaceScope(state, action)
      else -> state
    }
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

  private fun parseClass(initialState: State, tokenData: TokenData): State {
    var state = initialState

    if (
      tokenData.currentTokens.unconsumed.size > 2 &&
      tokenData.currentTokens.unconsumed[1].type == TokenType.OPENING_PAREN
    ) {
      if (tokenData.currentTokens.unconsumed.last().type != TokenType.OPENING_BRACE) {
        tokenData.currentTokens.consumeAll()

        return state.copy(
          errors = state.errors + CompileError(
            message = "Missing function declaration scope",
            tokenData = tokenData.unconsumed()
          )
        )
      }

      val name = tokenData.currentTokens.consumeFirst().value

      // opening paren
      tokenData.currentTokens.consumeFirst()

      val parameterTokens = mutableListOf(
        mutableListOf<Token>()
      )

      while (tokenData.currentTokens.isNotConsumed() && tokenData.currentTokens.unconsumed.first().type != TokenType.CLOSING_PAREN) {
        if (tokenData.currentTokens.unconsumed.first().type == TokenType.COMMA) {
          tokenData.currentTokens.consumeFirst()

          parameterTokens.add(mutableListOf())
        } else {
          parameterTokens.last().add(tokenData.currentTokens.consumeFirst())
        }
      }

      // closing paren
      tokenData.currentTokens.consumeFirst()

      if (tokenData.currentTokens.unconsumed.size > 1) {
        tokenData.currentTokens.consumeAllButLast()

        return state.copy(
          errors = state.errors + CompileError(
            message = "Invalid function declaration",
            tokenData = tokenData.consumeAllButLast()
          )
        )
      }

      tokenData.currentTokens.consumeFirst()

      val function = Function(name = name)

      state = dispatcher.dispatchAndExecute(
        state,
        AddFunctionAction(
          file = state.currentFile!!,
          clazz = state.currentClass!!,
          function = function
        )
      )

      state = parameterTokens.fold(state) { acc, tokens ->
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

      return state
    }

    return state
  }
}
