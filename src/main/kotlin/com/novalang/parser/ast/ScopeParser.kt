package com.novalang.parser.ast

import com.novalang.CompileError
import com.novalang.ast.Node
import com.novalang.ast.Scope
import com.novalang.parser.Dispatcher
import com.novalang.parser.actions.DispatcherAction
import com.novalang.parser.State
import com.novalang.parser.TokenData
import com.novalang.parser.TokenType
import com.novalang.parser.actions.AddAssignmentAction
import com.novalang.parser.actions.AddFunctionAction
import com.novalang.parser.actions.AddLocalDeclarationAction
import com.novalang.parser.actions.ClassParseAction
import com.novalang.parser.actions.FileParseAction
import com.novalang.parser.actions.ReplaceFunctionAction
import com.novalang.parser.actions.ReplaceScopeAction
import com.novalang.parser.actions.ScopeParseAction
import com.novalang.replace

class ScopeParser(dispatcher: Dispatcher) : Reducer(dispatcher) {
  override fun reduce(state: State, action: DispatcherAction): State {
    return when (action) {
      is FileParseAction -> parseFile(state, action.tokenData)
      is ClassParseAction -> parseFile(state, action.tokenData)
      is ScopeParseAction -> parseFile(state, action.tokenData)
      is AddLocalDeclarationAction -> addStatement(state, action.localDeclaration)
      is AddAssignmentAction -> addStatement(state, action.assignment)
      is AddFunctionAction -> addFunctionScope(state, action)
      is ReplaceScopeAction -> replaceScope(state, action)
      else -> state
    }
  }

  private fun replaceScope(initialState: State, action: ReplaceScopeAction): State {
    var state = initialState

    val index = state.scopes.indexOf(action.oldScope)

    if (index != -1) {
      state = state.copy(
        scopes = state.scopes.replace(action.oldScope, action.newScope)
      )

      // function level scope
      if (index == 0) {
        val newFunction = state.currentFunction!!.copy(
          scope = action.newScope
        )

        return dispatcher.dispatchAndExecute(
          state,
          ReplaceFunctionAction(
            file = state.currentFile!!,
            clazz = state.currentClass!!,
            oldFunction = state.currentFunction!!,
            newFunction = newFunction
          )
        )
      } else { // nested scope
        val oldScope = state.scopes[index - 1]
        val newScope = oldScope.copy(
          statements = oldScope.statements.replace(action.oldScope, action.newScope)
        )

        return dispatcher.dispatchAndExecute(
          state,
          ReplaceScopeAction(
            file = state.currentFile!!,
            clazz = state.currentClass!!,
            oldScope = oldScope,
            newScope = newScope
          )
        )
      }
    }

    return state
  }

  private fun addStatement(state: State, node: Node): State {
    val lastScope = state.scopes.last()

    val newScope = lastScope.copy(
      statements = lastScope.statements + node
    )

    return dispatcher.dispatchAndExecute(
      state,
      ReplaceScopeAction(
        file = state.currentFile!!,
        clazz = state.currentClass!!,
        oldScope = lastScope,
        newScope = newScope
      )
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

    val newFunction = action.function.copy(scope = Scope())

    return dispatcher.dispatchAndExecute(
      state.copy(
        scopes = state.scopes + newFunction.scope!!
      ),
      ReplaceFunctionAction(
        file = state.currentFile!!,
        clazz = state.currentClass!!,
        oldFunction = action.function,
        newFunction = newFunction
      )
    )
  }

  private fun parseFile(state: State, tokenData: TokenData): State {
    return when (tokenData.currentTokens.unconsumed[0].type) {
      TokenType.OPENING_BRACE -> parseScopeStart(state, tokenData)
      TokenType.CLOSING_BRACE -> parseScopeEnd(state, tokenData)
      else -> state
    }
  }

  private fun parseScopeStart(initialState: State, tokenData: TokenData): State {
    var state = initialState

    if (tokenData.currentTokens.unconsumed.size == 1) {
      tokenData.currentTokens.consumeAll()

      val lastScope = state.scopes.last()

      val scope = Scope()

      val newScope = lastScope.copy(
        statements = lastScope.statements + scope
      )

      state = state.copy(
        scopes = state.scopes + scope
      )

      return dispatcher.dispatchAndExecute(
        state,
        ReplaceScopeAction(
          file = state.currentFile!!,
          clazz = state.currentClass!!,
          oldScope = lastScope,
          newScope = newScope
        )
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
