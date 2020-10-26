package com.novalang.parser.ast

import com.novalang.CompileError
import com.novalang.ast.Field
import com.novalang.parser.Dispatcher
import com.novalang.parser.State
import com.novalang.parser.TokenData
import com.novalang.parser.TokenType
import com.novalang.parser.actions.AddFieldAction
import com.novalang.parser.actions.ClassParseAction
import com.novalang.parser.actions.DispatcherAction

class FieldParser(dispatcher: Dispatcher) : Reducer(dispatcher) {
  override fun reduce(state: State, action: DispatcherAction): State {
    return when (action) {
      is ClassParseAction -> parseField(state, action.tokenData)
      else -> state
    }
  }

  private fun parseField(state: State, tokenData: TokenData): State {
    return when (tokenData.tokens.unconsumed[0].type) {
      TokenType.LET -> parseConstant(state, tokenData)
      TokenType.VAR -> parseVariable(state, tokenData)
      else -> state
    }
  }

  private fun parseConstant(state: State, tokenData: TokenData): State {
    if (tokenData.tokens.unconsumed.size == 1) {
      tokenData.tokens.consumeAll()

      return state.copy(
        errors = state.errors + CompileError(
          message = "Missing constant declaration name",
          tokenData = tokenData.unconsumed()
        )
      )
    }

    tokenData.tokens.consumeFirst()

    return parseField(state, tokenData, true)
  }

  private fun parseVariable(state: State, tokenData: TokenData): State {
    if (tokenData.tokens.unconsumed.size == 1) {
      tokenData.tokens.consumeAll()

      return state.copy(
        errors = state.errors + CompileError(
          message = "Missing variable declaration name",
          tokenData = tokenData.unconsumed()
        )
      )
    }

    tokenData.tokens.consumeFirst()

    return parseField(state, tokenData, false)
  }

  private fun parseField(state: State, tokenData: TokenData, constant: Boolean): State {
    val nameToken = tokenData.tokens.consumeFirst()

    if (nameToken.type != TokenType.IDENTIFIER) {
      tokenData.tokens.consumeAll()

      return state.copy(
        errors = state.errors + CompileError(
          message = "Invalid variable declaration name",
          tokenData = tokenData.unconsumed()
        )
      )
    }

    if (tokenData.tokens.isConsumed()) {
      val field = Field(
        name = nameToken.value,
        type = null,
        constant = constant
      )

      return dispatcher.dispatchAndExecute(
        state,
        AddFieldAction(
          file = state.currentFile!!,
          clazz = state.currentClass!!,
          field = field
        )
      )
    }

    if (tokenData.tokens.consumeFirst().type != TokenType.COLON || tokenData.tokens.unconsumed.size > 1) {
      tokenData.tokens.consumeAll()

      return state.copy(
        errors = state.errors + CompileError(
          message = "Invalid field type declaration",
          tokenData = tokenData.unconsumed()
        )
      )
    }

    val typeToken = tokenData.tokens.consumeFirst()

    if (typeToken.type != TokenType.IDENTIFIER) {
      tokenData.tokens.consumeAll()

      return state.copy(
        errors = state.errors + CompileError(
          message = "Invalid parameter type",
          tokenData = tokenData.unconsumed()
        )
      )
    }

    val field = Field(
      name = nameToken.value,
      type = typeToken.value,
      constant = constant
    )

    return dispatcher.dispatchAndExecute(
      state,
      AddFieldAction(
        file = state.currentFile!!,
        clazz = state.currentClass!!,
        field = field
      )
    )
  }
}
