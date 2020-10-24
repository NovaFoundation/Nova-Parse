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
    if (tokenData.currentTokens.unconsumed[0].type == TokenType.IMPORT) {
      if (tokenData.currentTokens.unconsumed.size > 2) {
        tokenData.currentTokens.consumeAll()

        return state.copy(
          errors = state.errors + CompileError(
            message = "Too many arguments given to import statement",
            source = tokenData.source
          )
        )
      }
      if (tokenData.currentTokens.unconsumed.size == 1) {
        tokenData.currentTokens.consumeAll()

        return state.copy(
          errors = state.errors + CompileError(
            message = "Import location not specified",
            source = tokenData.source
          )
        )
      }

      val importValue = tokenData.currentTokens.unconsumed[1].value

      if (importValue.first() != '"' || importValue.last() != '"') {
        tokenData.currentTokens.consumeAll()

        return state.copy(
          errors = state.errors + CompileError(
            message = "Import location must be specified in quotes",
            source = tokenData.source
          )
        )
      }

      val importLocation = importValue.substring(1, importValue.length - 1)
      val value = Import(
        location = importLocation
      )

      if (state.currentFile!!.imports.any { it.location == importLocation }) {
        tokenData.currentTokens.consumeAll()

        return state.copy(
          errors = state.errors + CompileError(
            message = "Duplicate import \"${importLocation}\"",
            source = tokenData.source
          )
        )
      }

      val newFile = state.currentFile.copy(
        imports = state.currentFile.imports + value
      )

      tokenData.currentTokens.consumeAll()

      return state.copy(
        currentFile = newFile,
        files = state.files.replace(state.currentFile, newFile),
        imports = state.imports + value
      )
    }

    return state
  }
}
