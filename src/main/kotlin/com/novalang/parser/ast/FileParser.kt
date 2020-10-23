package com.novalang.parser.ast

import com.novalang.CompileError
import com.novalang.ast.File
import com.novalang.parser.Dispatcher
import com.novalang.parser.State
import com.novalang.parser.actions.AddClassAction
import com.novalang.parser.actions.DispatcherAction
import com.novalang.parser.actions.EndFileAction
import com.novalang.parser.actions.InitFileAction
import com.novalang.parser.actions.ReplaceClassAction
import com.novalang.parser.actions.ReplaceFileAction
import com.novalang.replace
import java.io.File as JavaFile

class FileParser(private val dispatcher: Dispatcher) : Reducer() {
  override fun reduce(state: State, action: DispatcherAction): State {
    return when (action) {
      is InitFileAction -> initFile(state, action.file)
      is EndFileAction -> finishFile(state)
      is ReplaceClassAction -> replaceClass(state, action)
      is ReplaceFileAction -> replaceFile(state, action)
      is AddClassAction -> addClass(state, action)
      else -> state
    }
  }

  private fun initFile(state: State, file: JavaFile): State {
    if (file.isDirectory) {
      return state.copy(
        errors = state.errors + CompileError(
          message = "Cannot parse a directory \"${file.canonicalPath}\"",
          source = ""
        )
      )
    }
    if (!file.isFile) {
      return state.copy(
        errors = state.errors + CompileError(
          message = "Invalid source file \"${file.canonicalPath}\"",
          source = ""
        )
      )
    }

    val fileNode = File(file)

    return state.copy(
      files = state.files + fileNode,
      currentFile = fileNode
    )
  }

  private fun finishFile(state: State): State {
    return state.copy(
      currentClass = null,
      currentFile = null
    )
  }

  private fun replaceClass(state: State, action: ReplaceClassAction): State {
    val newFile = action.file.copy(
      classes = action.file.classes.replace(action.oldClass, action.newClass)
    )

    dispatcher.dispatch(
      ReplaceFileAction(
        oldFile = action.file,
        newFile = newFile
      )
    )

    val currentClass = if (action.oldClass == state.currentClass) {
      action.newClass
    } else {
      state.currentClass
    }

    return state.copy(
      currentClass = currentClass
    )
  }

  private fun addClass(state: State, action: AddClassAction): State {
    val newFile = action.file.copy(
      classes = action.file.classes + action.newClass
    )

    dispatcher.dispatch(
      ReplaceFileAction(
        oldFile = action.file,
        newFile = newFile
      )
    )

    return state
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
