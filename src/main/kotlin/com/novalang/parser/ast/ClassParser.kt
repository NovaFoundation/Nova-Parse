package com.novalang.parser.ast

import com.novalang.ast.Class
import com.novalang.parser.Dispatcher
import com.novalang.parser.Pipeline
import com.novalang.parser.State
import com.novalang.parser.Token
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
    return reduceP(action).run(dispatcher, state, action.tokenData)
  }

  fun reduceP(action: DispatcherAction): Pipeline<*, *> {
    return when (action) {
      is FileParseAction -> parseClass()
      is AddFieldAction -> addField(action)
      is AddFunctionAction -> addFunction(action)
      is ReplaceFunctionAction -> replaceFunction(action)
      else -> Pipeline.create()
    }
  }

  private fun addField(action: AddFieldAction): Pipeline<*, *> {
    return Pipeline.create()
      .thenDoAction { state, _ ->
        val newClass = state.currentClass!!.copy(
          fields = state.currentClass.fields + action.field
        )

        ReplaceClassAction(
          oldClass = state.currentClass,
          newClass = newClass
        )
      }
  }

  private fun addFunction(action: AddFunctionAction): Pipeline<*, *> {
    return Pipeline.create()
      .thenSetState { it.copy(currentFunction = action.function) }

      .thenDoAction { state, _ ->
        val newClass = state.currentClass!!.copy(
          functions = state.currentClass.functions + action.function
        )

        ReplaceClassAction(
          oldClass = state.currentClass,
          newClass = newClass
        )
      }
  }

  private fun parseClass(): Pipeline<*, *> {
    lateinit var nameToken: Token

    return Pipeline.create()
      .thenExpectToken { (tokens) -> tokens.consumeFirstIfType(TokenType.CLASS) }

      .thenExpectToken { (tokens) -> tokens.consumeAtReverseIndexIfType(0, TokenType.OPENING_BRACE) }
      .orElseError("Class missing declaration scope")

      .thenExpectTokenCount(1)
      .orElseError { _, _, response -> if (response.actual > response.expected) "Invalid class declaration" else "Class missing name" }

      .thenExpectToken { (tokens) -> tokens.consumeFirstIfType(TokenType.IDENTIFIER) }
      .thenDoWithResponse { nameToken = it.token!! }
      .orElseError("Invalid class name")

      .thenSetState { it.copy(currentClass = Class(nameToken.value)) }

      .thenDoAction { state, _ ->
        AddClassAction(
          file = state.currentFile!!,
          newClass = state.currentClass!!
        )
      }
  }

  private fun replaceFunction(action: ReplaceFunctionAction): Pipeline<*, *> {
    return Pipeline.create()
      .thenDoAction { state, _ ->
        val newClass = state.currentClass!!.copy(
          functions = state.currentClass.functions.replace(action.oldFunction, action.newFunction)
        )

        ReplaceClassAction(
          oldClass = state.currentClass,
          newClass = newClass
        )
      }

      .thenSetState { state -> state.copy(currentFunction = action.newFunction) }
  }
}
