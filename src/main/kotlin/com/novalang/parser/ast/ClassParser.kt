package com.novalang.parser.ast

import com.novalang.CompileError
import com.novalang.ast.Class
import com.novalang.parser.Dispatcher
import com.novalang.parser.State
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

class ClassParser(private val dispatcher: Dispatcher) : Reducer() {
  override fun reduce(state: State, action: DispatcherAction): State {
    return when (action) {
      is FileParseAction -> parseFile(state, action.tokenData)
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

  private fun parseFile(state: State, tokenData: TokenData): State {
    if (tokenData.currentTokens.unconsumed[0].type == TokenType.CLASS) {
      if (tokenData.currentTokens.unconsumed.size < 3) {
        tokenData.currentTokens.consumeAll()

        return state.copy(
          errors = state.errors + CompileError(
            message = "Class name not specified",
            source = tokenData.source
          )
        )
      }
      if (tokenData.currentTokens.unconsumed.size > 3) {
        tokenData.currentTokens.consumeAll()

        return state.copy(
          errors = state.errors + CompileError(
            message = "Too many arguments given to class declaration",
            source = tokenData.source
          )
        )
      }
      if (tokenData.currentTokens.unconsumed[2].type != TokenType.OPENING_BRACE) {
        tokenData.currentTokens.consumeAll()

        return state.copy(
          errors = state.errors + CompileError(
            message = "Class declaration missing opening curly brace",
            source = tokenData.source
          )
        )
      }

      val classNameToken = tokenData.currentTokens.unconsumed[1]

      if (classNameToken.type != TokenType.IDENTIFIER) {
        tokenData.currentTokens.consumeAll()

        return state.copy(
          errors = state.errors + CompileError(
            message = "Invalid class name \"${classNameToken.value}\"",
            source = tokenData.source
          )
        )
      }

      val value = Class(
        name = classNameToken.value
      )

      tokenData.currentTokens.consumeAllButLast()

      return dispatcher.dispatchAndExecute(
        state.copy(
          currentClass = value
        ),
        AddClassAction(
          file = state.currentFile!!,
          newClass = value
        )
      )
    }

    return state
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
