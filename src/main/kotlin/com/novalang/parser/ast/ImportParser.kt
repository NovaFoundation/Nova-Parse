package com.novalang.parser.ast

import com.novalang.ast.Import
import com.novalang.parser.Dispatcher
import com.novalang.parser.Pipeline
import com.novalang.parser.State
import com.novalang.parser.TokenType
import com.novalang.parser.actions.DispatcherAction
import com.novalang.parser.actions.FileParseAction
import com.novalang.replace

class ImportParser(dispatcher: Dispatcher) : Reducer(dispatcher) {
  override fun reduce(state: State, action: DispatcherAction): State {
    return when (action) {
      is FileParseAction -> parseFile().run(dispatcher, state, action.tokenData)
      else -> state
    }
  }

  private fun parseFile(): Pipeline<*, *> {
    lateinit var importValue: String
    lateinit var importLocation: String

    return Pipeline.create()
      .thenExpectToken { (tokens) -> tokens.consumeFirstIfType(TokenType.IMPORT) }

      .thenCheck { _, (tokens) -> tokens.unconsumed.size > 1 }
      .ifTrueThenError("Too many arguments given to import statement")

      .thenCheck { _, (tokens) -> tokens.isConsumed() }
      .ifTrueThenError("Import location not specified")

      .thenDo { _, (tokens), _ -> importValue = tokens.consumeFirst().value }

      .thenCheck { _, _ -> importValue.first() != '"' || importValue.last() != '"' }
      .ifTrueThenError("Import location must be specified in quotes")

      .thenDo { _, _, _ -> importLocation = importValue.substring(1, importValue.length - 1) }

      .thenCheck { state, _ -> state.currentFile!!.imports.any { it.location == importLocation } }
      .ifTrueThenError { _, _, _ -> "Duplicate import \"${importLocation}\"" }

      .thenSetState { state ->
        val value = Import(
          location = importLocation
        )

        val newFile = state.currentFile!!.copy(
          imports = state.currentFile.imports + value
        )

        state.copy(
          currentFile = newFile,
          files = state.files.replace(state.currentFile, newFile),
          imports = state.imports + value
        )
      }
  }
}
