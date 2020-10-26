package com.novalang.parser.ast

import com.novalang.CompileError
import com.novalang.ast.Import
import com.novalang.parser.Dispatcher
import com.novalang.parser.State
import com.novalang.parser.actions.DispatcherAction
import com.novalang.parser.TokenData
import com.novalang.parser.TokenType
import com.novalang.parser.actions.FileParseAction
import com.novalang.replace

class ImportParser(dispatcher: Dispatcher) : Reducer(dispatcher) {
  override fun reduce(state: State, action: DispatcherAction): State {
    return when (action) {
      is FileParseAction -> parseFile(state, action.tokenData)
      else -> state
    }
  }

  private fun parseFile(state: State, tokenData: TokenData): State {
    if (tokenData.tokens.unconsumed[0].type == TokenType.IMPORT) {
      if (tokenData.tokens.unconsumed.size > 2) {
        tokenData.tokens.consumeAll()

        return state.copy(
          errors = state.errors + CompileError(
            message = "Too many arguments given to import statement",
            tokenData = tokenData.unconsumed()
          )
        )
      }
      if (tokenData.tokens.unconsumed.size == 1) {
        tokenData.tokens.consumeAll()

        return state.copy(
          errors = state.errors + CompileError(
            message = "Import location not specified",
            tokenData = tokenData.unconsumed()
          )
        )
      }

      val importValue = tokenData.tokens.unconsumed[1].value

      if (importValue.first() != '"' || importValue.last() != '"') {
        tokenData.tokens.consumeAll()

        return state.copy(
          errors = state.errors + CompileError(
            message = "Import location must be specified in quotes",
            tokenData = tokenData.unconsumed()
          )
        )
      }

      val importLocation = importValue.substring(1, importValue.length - 1)
      val value = Import(
        location = importLocation
      )

      if (state.currentFile!!.imports.any { it.location == importLocation }) {
        tokenData.tokens.consumeAll()

        return state.copy(
          errors = state.errors + CompileError(
            message = "Duplicate import \"${importLocation}\"",
            tokenData = tokenData.unconsumed()
          )
        )
      }

      val newFile = state.currentFile.copy(
        imports = state.currentFile.imports + value
      )

      tokenData.tokens.consumeAll()

      return state.copy(
        currentFile = newFile,
        files = state.files.replace(state.currentFile, newFile),
        imports = state.imports + value
      )
    }

    return state
  }
}
