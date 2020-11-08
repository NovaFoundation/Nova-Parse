package com.novalang.parser.ast

import com.novalang.ast.File
import com.novalang.parser.Dispatcher
import com.novalang.parser.Pipeline
import com.novalang.parser.State
import com.novalang.parser.actions.AddClassAction
import com.novalang.parser.actions.DispatcherAction
import com.novalang.parser.actions.EndFileAction
import com.novalang.parser.actions.InitFileAction
import com.novalang.parser.actions.ReplaceClassAction
import com.novalang.parser.actions.ReplaceFileAction
import com.novalang.replace

class FileParser(dispatcher: Dispatcher) : Reducer(dispatcher) {
  override fun reduce(state: State, action: DispatcherAction): State {
    return when (action) {
      is InitFileAction -> initFile(action).run(dispatcher, state, action.tokenData)
      is EndFileAction -> finishFile(state)
      is ReplaceClassAction -> replaceClass(state, action)
      is ReplaceFileAction -> replaceFile(state, action)
      is AddClassAction -> addClass(state, action)
      else -> state
    }
  }

  private fun initFile(action: InitFileAction): Pipeline<*, *> {
    return Pipeline.create()
      .thenCheck { _, _ -> action.file.isDirectory }
      .ifTrueThenSetState { state -> state.copy(currentFile = null) }
      .ifTrueThenError("Cannot parse a directory \"${action.file.canonicalPath}\"")

      .thenCheck { _, _ -> !action.file.isFile }
      .ifTrueThenSetState { state -> state.copy(currentFile = null) }
      .ifTrueThenError("Invalid source file \"${action.file.canonicalPath}\"")

      .thenSetState { state ->
        val fileNode = File(action.file)

        state.copy(
          files = state.files + fileNode,
          currentFile = fileNode
        )
      }
  }

  private fun finishFile(state: State): State {
    return state.copy(
      currentClass = null,
      currentFile = null
    )
  }

  private fun replaceClass(initialState: State, action: ReplaceClassAction): State {
    var state = initialState

    val newFile = state.currentFile!!.copy(
      classes = state.currentFile!!.classes.replace(action.oldClass, action.newClass)
    )

    state = dispatcher.dispatchAndExecute(
      state,
      ReplaceFileAction(
        oldFile = state.currentFile!!,
        newFile = newFile
      )
    )

    return state.copy(
      currentClass = action.newClass
    )
  }

  private fun addClass(state: State, action: AddClassAction): State {
    val newFile = action.file.copy(
      classes = action.file.classes + action.newClass
    )

    return dispatcher.dispatchAndExecute(
      state,
      ReplaceFileAction(
        oldFile = action.file,
        newFile = newFile
      )
    )
  }

  private fun replaceFile(state: State, action: ReplaceFileAction): State {
    val currentFile = if (action.oldFile == state.currentFile) {
      action.newFile
    } else {
      state.currentFile
    }

    return state.copy(
      currentFile = currentFile,
      files = state.files.replace(action.oldFile, action.newFile)
    )
  }
}
