package com.novalang.parser.ast

import com.novalang.ast.Scope
import com.novalang.parser.Dispatcher
import com.novalang.parser.actions.DispatcherAction
import com.novalang.parser.State
import com.novalang.parser.TokenData
import com.novalang.parser.TokenType
import com.novalang.parser.actions.ClassParseAction
import com.novalang.parser.actions.FileParseAction
import com.novalang.parser.actions.ReplaceClassAction

class ScopeParser(private val dispatcher: Dispatcher) : Reducer() {
  override fun reduce(state: State, action: DispatcherAction): State {
    return when (action) {
      is FileParseAction -> parseFile(state, action.tokenData)
      is ClassParseAction -> parseFile(state, action.tokenData)
      else -> state
    }
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

      val newClass = state.currentClass!!.copy(
        scope = scope
      )

      return dispatcher.dispatchAndExecute(
        state.copy(
          scopes = state.scopes + scope
        ),
        ReplaceClassAction(
          file = state.currentFile!!,
          oldClass = state.currentClass,
          newClass = newClass
        )
      )
    }

    return state
  }

  private fun parseScopeEnd(state: State, tokenData: TokenData): State {
    if (tokenData.currentTokens.unconsumed.size == 1) {
      tokenData.currentTokens.consumeAll()

      val newScopes = state.scopes.drop(1)

      val currentClass = if (newScopes.isEmpty()) {
        null
      } else {
        state.currentClass
      }

      return state.copy(
        scopes = newScopes,
        currentClass = currentClass
      )
    }

    return state
  }


}
