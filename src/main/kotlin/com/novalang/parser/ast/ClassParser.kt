package com.novalang.parser.ast

import com.novalang.ast.Class
import com.novalang.parser.Dispatcher
import com.novalang.parser.Pipeline
import com.novalang.parser.State
import com.novalang.parser.Token
import com.novalang.parser.TokenData
import com.novalang.parser.TokenType
import com.novalang.parser.actions.AddClassAction
import com.novalang.parser.actions.AddFieldAction
import com.novalang.parser.actions.AddFunctionAction
import com.novalang.parser.actions.DispatcherAction
import com.novalang.parser.actions.FileParseAction
import com.novalang.parser.actions.ReplaceClassAction
import com.novalang.parser.actions.ReplaceFunctionAction
import com.novalang.replace

class ClassParser(dispatcher: Dispatcher) : Reducer(dispatcher) {
  override fun reduce(state: State, action: DispatcherAction): State {
    return when (action) {
      is FileParseAction -> parseClass(state, action.tokenData)
      is AddFieldAction -> addField(state, action)
      is AddFunctionAction -> addFunction(state, action)
      is ReplaceFunctionAction -> replaceFunction(state, action)
      else -> state
    }
  }

  private fun addField(state: State, action: AddFieldAction): State {
    val newClass = action.clazz.copy(
      fields = action.clazz.fields + action.field
    )

    return dispatcher.dispatchAndExecute(
      state,
      ReplaceClassAction(
        file = action.file,
        oldClass = action.clazz,
        newClass = newClass
      )
    )
  }

  private fun addFunction(state: State, action: AddFunctionAction): State {
    val newClass = action.clazz.copy(
      functions = action.clazz.functions + action.function
    )

    return dispatcher.dispatchAndExecute(
      state.copy(
        currentFunction = action.function
      ),
      ReplaceClassAction(
        file = action.file,
        oldClass = action.clazz,
        newClass = newClass
      )
    )
  }

  private fun parseClass(initialState: State, tokenData: TokenData): State {
    lateinit var nameToken: Token

    return Pipeline.create()
      .thenExpectToken { it.consumeFirstIfType(TokenType.CLASS) }

      .thenExpectToken { it.consumeAtReverseIndexIfType(0, TokenType.OPENING_BRACE) }
      .orElseError { "Class missing declaration scope" }

      .thenExpectTokenCount(1)
      .orElseError { if (it.actual > it.expected) "Invalid class declaration" else "Class missing name" }

      .thenExpectToken { it.consumeFirstIfType(TokenType.IDENTIFIER) }
      .thenDo { nameToken = it.token!! }
      .orElseError { "Invalid class name" }

      .thenSetState { it.copy(currentClass = Class(nameToken.value)) }

      .thenDoAction { state, _ ->
        AddClassAction(
          file = state.currentFile!!,
          newClass = state.currentClass!!
        )
      }

      .run(dispatcher, initialState, tokenData)
  }

  private fun replaceFunction(initialState: State, action: ReplaceFunctionAction): State {
    val newClass = action.clazz.copy(
      functions = action.clazz.functions.replace(action.oldFunction, action.newFunction)
    )

    val state = dispatcher.dispatchAndExecute(
      initialState,
      ReplaceClassAction(
        file = action.file,
        oldClass = action.clazz,
        newClass = newClass
      )
    )

    val currentFunction = if (action.oldFunction == state.currentFunction) {
      action.newFunction
    } else {
      state.currentFunction
    }

    return state.copy(
      currentFunction = currentFunction
    )
  }
}
