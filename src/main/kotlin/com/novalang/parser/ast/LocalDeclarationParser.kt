package com.novalang.parser.ast

import com.novalang.CompileError
import com.novalang.ast.LocalDeclaration
import com.novalang.parser.Dispatcher
import com.novalang.parser.State
import com.novalang.parser.TokenData
import com.novalang.parser.TokenType
import com.novalang.parser.actions.AddLocalDeclarationAction
import com.novalang.parser.actions.DispatcherAction
import com.novalang.parser.actions.ScopeParseAction

class LocalDeclarationParser(private val dispatcher: Dispatcher) : Reducer() {
  override fun reduce(state: State, action: DispatcherAction): State {
    return when (action) {
      is ScopeParseAction -> parseLocalDeclaration(state, action.tokenData)
      else -> state
    }
  }

  private fun parseLocalDeclaration(state: State, tokenData: TokenData): State {
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
      val localDeclaration = LocalDeclaration(
        name = tokenData.currentTokens.unconsumed[1].value,
        type = null,
        constant = true
      )

      tokenData.currentTokens.consumeAll()

      return dispatcher.dispatchAndExecute(
        state,
        AddLocalDeclarationAction(
          file = state.currentFile!!,
          clazz = state.currentClass!!,
          localDeclaration = localDeclaration
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
      val localDeclaration = LocalDeclaration(
        name = tokenData.currentTokens.unconsumed[1].value,
        type = null,
        constant = false
      )

      tokenData.currentTokens.consumeAll()

      return dispatcher.dispatchAndExecute(
        state,
        AddLocalDeclarationAction(
          file = state.currentFile!!,
          clazz = state.currentClass!!,
          localDeclaration = localDeclaration
        )
      )
    }

    return state
  }
}
