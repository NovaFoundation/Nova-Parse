package com.novalang.parser.ast

import com.novalang.CompileError
import com.novalang.ast.Field
import com.novalang.ast.Parameter
import com.novalang.parser.Dispatcher
import com.novalang.parser.State
import com.novalang.parser.TokenType
import com.novalang.parser.actions.AddFieldAction
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
    return when (action.tokenData.currentTokens.unconsumed[0].type) {
      TokenType.LET -> parseConstant(state, action)
      TokenType.VAR -> parseVariable(state, action)
      else -> parseConstant(state, action)
    }
  }

  private fun parseConstant(state: State, action: ParameterParseAction): State {
    val currentTokens = action.tokenData.currentTokens

    if (currentTokens.unconsumed.first().type == TokenType.LET) {
      currentTokens.consumeFirst()
    }

    return parseParameterData(state, action, true)
  }

  private fun parseVariable(state: State, action: ParameterParseAction): State {
    val currentTokens = action.tokenData.currentTokens

    if (currentTokens.unconsumed.first().type == TokenType.VAR) {
      currentTokens.consumeFirst()
    }

    return parseParameterData(state, action, false)
  }

  private fun parseParameterData(state: State, action: ParameterParseAction, constant: Boolean): State {
    val currentTokens = action.tokenData.currentTokens

    val parameterNameToken = currentTokens.consumeFirst()

    if (parameterNameToken.type != TokenType.IDENTIFIER) {
      currentTokens.consumeAll()

      return state.copy(
        errors = state.errors + CompileError(
          message = "Missing parameter name",
          source = action.tokenData.source
        )
      )
    }

    if (currentTokens.isConsumed()) {
      val newParameter = Parameter(
        name = parameterNameToken.value,
        type = null,
        constant = constant
      )

      return dispatcher.dispatchAndExecute(
        state,
        AddParameterAction(
          file = state.currentFile!!,
          clazz = state.currentClass!!,
          function = action.function,
          parameter = newParameter
        )
      )
    }

    if (currentTokens.consumeFirst().type != TokenType.COLON || currentTokens.unconsumed.size > 1) {
      currentTokens.consumeAll()

      return state.copy(
        errors = state.errors + CompileError(
          message = "Invalid parameter type declaration",
          source = action.tokenData.source
        )
      )
    }

    val typeToken = currentTokens.consumeFirst()

    if (typeToken.type != TokenType.IDENTIFIER) {
      currentTokens.consumeAll()

      return state.copy(
        errors = state.errors + CompileError(
          message = "Invalid parameter type",
          source = action.tokenData.source
        )
      )
    }

    val newParameter = Parameter(
      name = parameterNameToken.value,
      type = typeToken.value,
      constant = constant
    )

    return dispatcher.dispatchAndExecute(
      state,
      AddParameterAction(
        file = state.currentFile!!,
        clazz = state.currentClass!!,
        function = action.function,
        parameter = newParameter
      )
    )
  }
}
