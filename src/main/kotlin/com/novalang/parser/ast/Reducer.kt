package com.novalang.parser.ast

import com.novalang.ast.File
import com.novalang.ast.Class
import com.novalang.ast.Function
import com.novalang.ast.Node
import com.novalang.ast.Scope
import com.novalang.parser.State
import com.novalang.parser.actions.DispatcherAction
import com.novalang.parser.actions.ReplaceClassAction
import com.novalang.parser.actions.ReplaceFileAction
import com.novalang.parser.actions.ReplaceFunctionAction
import com.novalang.parser.actions.ReplaceScopeAction

abstract class Reducer {
  abstract fun reduce(state: State, action: DispatcherAction): State

  fun replaceNodeAction(state: State, oldNode: Node, newNode: Node): DispatcherAction {
    return when {
      oldNode is File && newNode is File -> ReplaceFileAction(oldNode, newNode)
      oldNode is Class && newNode is Class -> ReplaceClassAction(state.currentFile!!, oldNode, newNode)
      oldNode is Function && newNode is Function -> ReplaceFunctionAction(state.currentFile!!, state.currentClass!!, oldNode, newNode)
      oldNode is Scope && newNode is Scope -> ReplaceScopeAction(state.currentFile!!, state.currentClass!!, state.scopes.last(), oldNode, newNode)
      else -> throw UnsupportedOperationException("Invalid node types")
    }
  }
}
