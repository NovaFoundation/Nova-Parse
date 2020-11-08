package com.novalang.parser.ast

import com.novalang.ast.ElseStatement
import com.novalang.parser.Dispatcher
import com.novalang.parser.Pipeline
import com.novalang.parser.State
import com.novalang.parser.TokenType
import com.novalang.parser.actions.AddElseStatementAction
import com.novalang.parser.actions.AddElseStatementIfStatementAction
import com.novalang.parser.actions.DispatcherAction
import com.novalang.parser.actions.ElseStatementIfStatementParseAction
import com.novalang.parser.actions.ScopeParseAction

class ElseStatementParser(dispatcher: Dispatcher) : Reducer(dispatcher) {
  override fun reduce(state: State, action: DispatcherAction): State {
    return when (action) {
      is ScopeParseAction -> parseElseStatement().run(dispatcher, state, action.tokenData)
      is AddElseStatementIfStatementAction -> addElseStatementValue(action).run(dispatcher, state, action.tokenData)
      else -> state
    }
  }

  private fun addElseStatementValue(action: AddElseStatementIfStatementAction): Pipeline<*, *> {
    return Pipeline.create()
      .thenDoAction { state, _ ->
        AddElseStatementAction(
          file = state.currentFile!!,
          clazz = state.currentClass!!,
          elseStatement = state.currentElseStatement!!.copy(ifStatement = action.ifStatement)
        )
      }

      .thenSetState { state -> state.copy(currentElseStatement = null) }
  }

  private fun parseElseStatement(): Pipeline<*, *> {
    return Pipeline.create()
      .thenExpectToken { (tokens) -> tokens.consumeFirstIfType(TokenType.ELSE) }

      .thenExpectToken { (tokens) -> tokens.consumeLastIfType(TokenType.OPENING_BRACE) }
      .orElseError("Else statement missing opening brace")

      .thenCheck { _, (tokens) -> tokens.isConsumed() }

      .ifTrueThenDoAction { state, _ ->
        val elseStatement = ElseStatement()

        AddElseStatementAction(
          file = state.currentFile!!,
          clazz = state.currentClass!!,
          elseStatement = elseStatement
        )
      }

      .ifFalse { it, _ ->
        it.thenSetState { state ->
          val elseStatement = ElseStatement()

          state.copy(currentElseStatement = elseStatement)
        }

          .thenDoAction { _, tokens -> ElseStatementIfStatementParseAction(tokenData = tokens) }
      }
  }
}
