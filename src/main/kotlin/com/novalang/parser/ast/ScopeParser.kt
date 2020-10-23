package com.novalang.parser.ast

import com.novalang.CompileError
import com.novalang.ast.Scope
import com.novalang.parser.Dispatcher
import com.novalang.parser.actions.DispatcherAction
import com.novalang.parser.State
import com.novalang.parser.TokenData
import com.novalang.parser.TokenType
import com.novalang.parser.actions.AddFunctionAction
import com.novalang.parser.actions.ClassParseAction
import com.novalang.parser.actions.FileParseAction
import com.novalang.parser.actions.ReplaceFunctionAction
import com.novalang.parser.actions.ScopeParseAction
import com.novalang.replace

class ScopeParser(private val dispatcher: Dispatcher) : Reducer() {
  override fun reduce(state: State, action: DispatcherAction): State {
    return when (action) {
      is FileParseAction -> parseFile(state, action.tokenData)
      is ClassParseAction -> parseFile(state, action.tokenData)
      is ScopeParseAction -> parseFile(state, action.tokenData)
      is AddFunctionAction -> addFunctionScope(state, action)
      is ReplaceFunctionAction -> replaceFunction(state, action)
      else -> state
    }
  }

  private fun replaceFunction(state: State, action: ReplaceFunctionAction): State {
    return state.copy(
      scopes = state.scopes.replace(action.oldFunction, action.newFunction)
    )
  }

  private fun addFunctionScope(state: State, action: AddFunctionAction): State {
    if (state.scopes.isNotEmpty()) {
      return state.copy(
        errors = state.errors + CompileError(
          message = "Nested functions are not allowed",
          source = ""
        )
      )
    }

    return state.copy(
      scopes = state.scopes + action.function
    )
  }

  private fun parseFile(state: State, tokenData: TokenData): State {
    return when (tokenData.currentTokens.unconsumed[0].type) {
      TokenType.OPENING_BRACE -> parseScopeStart(state, tokenData)
      TokenType.CLOSING_BRACE -> parseScopeEnd(state, tokenData)
      else -> state
    }
  }

  private fun parseScopeStart(state: State, tokenData: TokenData): State {
    if (tokenData.currentTokens.unconsumed.size == 1) {
      tokenData.currentTokens.consumeAll()

      val scope = Scope()
      val lastScope = state.scopes.last()

      // if nested scope
      val newScope = if (lastScope.scope != null) {
        lastScope.setScope(lastScope.scope!!.setScope(scope) as Scope)
      } else {
        lastScope.setScope(scope)
      }

      return dispatcher.dispatchAndExecute(
        state.copy(
          scopes = state.scopes.replace(
            lastScope,
            newScope
          ) + scope
        ),
        replaceNodeAction(state, lastScope, newScope)
      )
    }

    return state
  }

  private fun parseScopeEnd(state: State, tokenData: TokenData): State {
    if (tokenData.currentTokens.unconsumed.size == 1) {
      tokenData.currentTokens.consumeAll()

      if (state.currentClass == null) {
        return state.copy(
          errors = state.errors + CompileError(
            message = "Unexpected closing curly brace",
            source = tokenData.source
          )
        )
      } else if (state.scopes.isEmpty()) {
        return state.copy(
          currentClass = null
        )
      }

      val newScopes = state.scopes.drop(1)

      val currentFunction = if (newScopes.isEmpty()) {
        null
      } else {
        state.currentFunction
      }

      return state.copy(
        scopes = newScopes,
        currentFunction = currentFunction
      )
    }

    return state
  }
}
