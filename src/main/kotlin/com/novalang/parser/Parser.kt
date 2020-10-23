package com.novalang.parser

import com.novalang.CompileError
import com.novalang.parser.actions.ClassParseAction
import com.novalang.parser.actions.EndFileAction
import com.novalang.parser.actions.InitFileAction
import com.novalang.parser.actions.ParseAction
import com.novalang.parser.actions.FileParseAction
import com.novalang.parser.actions.ScopeParseAction
import java.io.File

class Parser(
  private val dispatcher: Dispatcher = Dispatcher()
) {
  fun parseFile(initialState: State, file: File): State {
    var state = initialState.copy(
      parsedFiles = initialState.parsedFiles + file
    )

    state = dispatcher.dispatchAndExecuteAll(
      state,
      InitFileAction(
        file = file
      )
    )

    if (state.currentFile == null) {
      return state
    }

    val tokenizer = Tokenizer(
      input = file.bufferedReader()
    )

    state = tokenizer.asSequence().fold(state) { acc, tokenData ->
      dispatcher.dispatchAndExecuteAll(acc, getParseAction(acc, tokenData))
    }

    if (state.scopes.isNotEmpty()) {
      state = state.copy(
        errors = state.errors + CompileError(
          message = "Missing ending brace",
          source = ""
        )
      )
    }

    return dispatcher.dispatchAndExecuteAll(
      state,
      EndFileAction()
    )
  }

  private fun getParseAction(state: State, tokenData: TokenData): ParseAction {
    return when {
      state.scopes.size > 1 -> ScopeParseAction(tokenData)
      state.currentClass != null -> ClassParseAction(tokenData)
      else -> FileParseAction(tokenData)
    }
  }
}
