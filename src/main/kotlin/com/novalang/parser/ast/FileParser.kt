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
      is EndFileAction -> finishFile().run(dispatcher, state, action.tokenData)
      is ReplaceClassAction -> replaceClass(action).run(dispatcher, state, action.tokenData)
      is ReplaceFileAction -> replaceFile(action).run(dispatcher, state, action.tokenData)
      is AddClassAction -> addClass(action).run(dispatcher, state, action.tokenData)
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

  private fun finishFile(): Pipeline<*, *> {
    return Pipeline.create()
      .thenSetState {
        it.copy(
          currentClass = null,
          currentFile = null
        )
      }
  }

  private fun replaceClass(action: ReplaceClassAction): Pipeline<*, *> {
    return Pipeline.create()
      .thenSetState { it.copy(currentClass = action.newClass) }
      .thenDoAction { state, _ ->
        val newFile = state.currentFile!!.copy(
          classes = state.currentFile.classes.replace(action.oldClass, action.newClass)
        )

        ReplaceFileAction(
          oldFile = state.currentFile,
          newFile = newFile
        )
      }
  }

  private fun addClass(action: AddClassAction): Pipeline<*, *> {
    return Pipeline.create()
      .thenDoAction { _, _ ->
        val newFile = action.file.copy(
          classes = action.file.classes + action.newClass
        )

        ReplaceFileAction(
          oldFile = action.file,
          newFile = newFile
        )
      }
  }

  private fun replaceFile(action: ReplaceFileAction): Pipeline<*, *> {
    return Pipeline.create()
      .thenSetState { state ->
        state.copy(
          currentFile = action.newFile,
          files = state.files.replace(action.oldFile, action.newFile)
        )
      }
  }
}
