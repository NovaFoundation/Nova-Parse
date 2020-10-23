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

class FieldParser(private val dispatcher: Dispatcher) : Reducer() {
  override fun reduce(state: State, action: DispatcherAction): State {
    return when (action) {
      is ClassParseAction -> parseClass(state, action.tokenData)
      else -> state
    }
  }

  private fun parseClass(state: State, tokenData: TokenData): State {
    return when (tokenData.currentTokens.unconsumed[0].type) {
      TokenType.LET -> parseConstant(state, tokenData)
      TokenType.VAR -> parseVariable(state, tokenData)
      else -> state
    }
  }

  private fun parseConstant(state: State, tokenData: TokenData): State {
    if (tokenData.currentTokens.unconsumed.size == 1) {
      tokenData.currentTokens.consumeAll()

      return state.copy(
        errors = state.errors + CompileError(
          message = "Missing constant declaration name",
          source = tokenData.source
        )
      )
    }

    if (tokenData.currentTokens.unconsumed[1].type != TokenType.IDENTIFIER) {
      tokenData.currentTokens.consumeAll()

      return state.copy(
        errors = state.errors + CompileError(
          message = "Invalid constant declaration name",
          source = tokenData.source
        )
      )
    }

    if (tokenData.currentTokens.unconsumed.size == 2) {
      val field = Field(
        name = tokenData.currentTokens.unconsumed[1].value,
        constant = true
      )

      tokenData.currentTokens.consumeAll()

      return dispatcher.dispatchAndExecute(
        state,
        AddFieldAction(
          file = state.currentFile!!,
          clazz = state.currentClass!!,
          field = field
        )
      )
    }

    return state
  }

  private fun parseVariable(state: State, tokenData: TokenData): State {
    if (tokenData.currentTokens.unconsumed.size == 1) {
      tokenData.currentTokens.consumeAll()

      return state.copy(
        errors = state.errors + CompileError(
          message = "Missing variable declaration name",
          source = tokenData.source
        )
      )
    }

    if (tokenData.currentTokens.unconsumed[1].type != TokenType.IDENTIFIER) {
      tokenData.currentTokens.consumeAll()

      return state.copy(
        errors = state.errors + CompileError(
          message = "Invalid variable declaration name",
          source = tokenData.source
        )
      )
    }

    if (tokenData.currentTokens.unconsumed.size == 2) {
      val field = Field(
        name = tokenData.currentTokens.unconsumed[1].value,
        constant = false
      )

      tokenData.currentTokens.consumeAll()

      return dispatcher.dispatchAndExecute(
        state,
        AddFieldAction(
          file = state.currentFile!!,
          clazz = state.currentClass!!,
          field = field
        )
      )
    }

    return state
  }
}
