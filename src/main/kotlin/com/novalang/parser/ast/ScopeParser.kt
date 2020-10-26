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
import com.novalang.parser.actions.AddElseStatementAction
import com.novalang.parser.actions.AddFunctionAction
import com.novalang.parser.actions.AddIfStatementAction
import com.novalang.parser.actions.AddLocalDeclarationAction
import com.novalang.parser.actions.ClassParseAction
import com.novalang.parser.actions.EndScopeAction
import com.novalang.parser.actions.FileParseAction
import com.novalang.parser.actions.ReplaceFunctionAction
import com.novalang.parser.actions.ReplaceScopeAction
import com.novalang.parser.actions.ReplaceStatementAction
import com.novalang.parser.actions.ScopeParseAction
import com.novalang.parser.actions.StartScopeAction
import com.novalang.replace

class ScopeParser(dispatcher: Dispatcher) : Reducer(dispatcher) {
  override fun reduce(state: State, action: DispatcherAction): State {
    return when (action) {
      is FileParseAction -> parseFile(state, action.tokenData)
      is ClassParseAction -> parseFile(state, action.tokenData)
      is ScopeParseAction -> parseFile(state, action.tokenData)
      is StartScopeAction -> startScope(state, action)
      is EndScopeAction -> endScope(state, action)
      is ReplaceStatementAction -> replaceStatement(state, action)
      is AddLocalDeclarationAction -> addStatement(state, action.localDeclaration)
      is AddAssignmentAction -> addStatement(state, action.assignment)
      is AddIfStatementAction -> addIfStatementScope(state, action)
      is AddElseStatementAction -> addElseStatementScope(state, action)
      is ReplaceScopeAction -> replaceScope(state, action)
      else -> state
    }
  }

  private fun startScope(state: State, action: StartScopeAction): State {
    val lastScope = state.scopes.lastOrNull()
    val scope = Scope()

    if (lastScope == null) {
      val newFunction = state.currentFunction?.copy(scope = scope) ?: return error(state, action.tokenData, "No current function to add a scope to")

      return dispatcher.dispatchAndExecute(
        state = state.copy(
          scopes = state.scopes + scope
        ),
        action = ReplaceFunctionAction(
          file = state.currentFile!!,
          clazz = state.currentClass!!,
          oldFunction = state.currentFunction,
          newFunction = newFunction
        )
      )
    } else {
      val newScope = lastScope.copy(
        statements = lastScope.statements + scope
      )

      return dispatcher.dispatchAndExecute(
        state = state.copy(
          scopes = state.scopes + scope
        ),
        action = ReplaceScopeAction(
          file = state.currentFile!!,
          clazz = state.currentClass!!,
          oldScope = lastScope,
          newScope = newScope
        )
      )
    }
  }

  private fun endScope(state: State, action: EndScopeAction): State {
    return state.copy(
      scopes = state.scopes - action.scope
    )
  }

  private fun replaceScope(initialState: State, action: ReplaceScopeAction): State {
    var state = initialState

    val index = state.scopes.indexOf(action.oldScope)

    if (index != -1) {
      state = state.copy(
        scopes = state.scopes.replace(action.oldScope, action.newScope)
      )

      // nested scope
      if (index > 0) {
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

  private fun replaceStatement(state: State, action: ReplaceStatementAction): State {
    val lastScope = state.scopes.last()

    val newScope = lastScope.copy(
      statements = lastScope.statements.replace(action.oldStatement, action.newStatement)
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

  private fun addIfStatementScope(state: State, action: AddIfStatementAction): State {
    return addStatement(state, action.ifStatement).copy(
      scopes = state.scopes + action.ifStatement.scope!!
    )
  }

  private fun addElseStatementScope(state: State, action: AddElseStatementAction): State {
    val scope = action.elseStatement.scope ?: action.elseStatement.ifStatement!!.scope

    return addStatement(state, action.elseStatement).copy(
      scopes = state.scopes + scope!!
    )
  }

  private fun parseFile(state: State, tokenData: TokenData): State {
    return when (tokenData.currentTokens.unconsumed.first().type) {
      TokenType.OPENING_BRACE -> parseScopeStart(state, tokenData)
      TokenType.CLOSING_BRACE -> parseScopeEnd(state, tokenData)
      else -> state
    }
  }

  private fun parseScopeStart(state: State, tokenData: TokenData): State {
    if (tokenData.currentTokens.unconsumed.size == 1) {
      tokenData.currentTokens.consumeAll()

      return dispatcher.dispatchAndExecute(
        state = state,
        action = StartScopeAction(
          tokenData = tokenData
        )
      )
    }

    return state
  }

  private fun parseScopeEnd(state: State, tokenData: TokenData): State {
    tokenData.currentTokens.consumeFirst()

    if (state.currentClass == null) {
      return state.copy(
        errors = state.errors + CompileError(
          message = "Unexpected closing curly brace",
          tokenData = tokenData.unconsumed()
        )
      )
    } else if (state.scopes.isEmpty()) {
      return state.copy(
        currentClass = null
      )
    }

    return dispatcher.dispatchAndExecute(
      state,
      EndScopeAction(
        scope = state.scopes.last(),
        tokenData = tokenData
      )
    )
  }
}
