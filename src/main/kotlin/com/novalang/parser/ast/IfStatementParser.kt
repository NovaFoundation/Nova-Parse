package com.novalang.parser.ast

import com.novalang.ast.IfStatement
import com.novalang.ast.Literal
import com.novalang.parser.Dispatcher
import com.novalang.parser.Pipeline
import com.novalang.parser.State
import com.novalang.parser.TokenData
import com.novalang.parser.TokenType
import com.novalang.parser.actions.AddElseStatementIfStatementAction
import com.novalang.parser.actions.AddIfStatementAction
import com.novalang.parser.actions.AddIfStatementValueAction
import com.novalang.parser.actions.DispatcherAction
import com.novalang.parser.actions.ElseStatementIfStatementParseAction
import com.novalang.parser.actions.EndScopeAction
import com.novalang.parser.actions.IfStatementValueParseAction
import com.novalang.parser.actions.ReplaceStatementAction
import com.novalang.parser.actions.ReplaceScopeAction
import com.novalang.parser.actions.ScopeParseAction

class IfStatementParser(dispatcher: Dispatcher) : Reducer(dispatcher) {
  override fun reduce(state: State, action: DispatcherAction): State {
    return when (action) {
      is ScopeParseAction -> parseIfStatement(action.tokenData).run(dispatcher, state, action.tokenData)
      is ReplaceScopeAction -> replaceScope(action).run(dispatcher, state, action.tokenData)
      is ElseStatementIfStatementParseAction -> parseIfStatementForElseStatement(action).run(dispatcher, state, action.tokenData)
      is AddIfStatementValueAction -> addIfStatementValue(action).run(dispatcher, state, action.tokenData)
      is AddIfStatementAction -> addIfStatement(action).run(dispatcher, state, action.tokenData)
      is EndScopeAction -> endScope(action).run(dispatcher, state, action.tokenData)
      else -> state
    }
  }

  private fun endScope(action: EndScopeAction): Pipeline<*, *> {
    return Pipeline.create()
      .thenCheck { state, _ -> state.currentIfStatement?.scope }
      .ifEquals(action.scope) { it, _ ->
        it.thenSetState { state ->
          val newIfStatements = state.ifStatements - state.currentIfStatement!!

          state.copy(
            currentIfStatement = newIfStatements.lastOrNull(),
            ifStatements = newIfStatements
          )
        }
      }
  }

  private fun addIfStatement(action: AddIfStatementAction): Pipeline<*, *> {
    return Pipeline.create()
      .thenSetState { state ->
        state.copy(
          currentIfStatement = action.ifStatement,
          ifStatements = state.ifStatements + action.ifStatement
        )
      }
  }

  private fun replaceScope(action: ReplaceScopeAction): Pipeline<*, *> {
    return Pipeline.create()
      .thenCheck { state, _ -> state.ifStatements.find { it.scope == action.oldScope } }
      .ifNotEquals(null) { it, response ->
        val ifStatement = response.value as IfStatement

        it.thenDoAction { _, _ ->
            ReplaceStatementAction(
              oldStatement = ifStatement,
              newStatement = ifStatement.copy(scope = action.newScope)
            )
          }
      }
  }

  private fun parseIfStatementForElseStatement(action: ElseStatementIfStatementParseAction): Pipeline<*, *> {
    return parseIfStatement(action.tokenData)
  }

  private fun addIfStatementValue(action: AddIfStatementValueAction): Pipeline<*, *> {
    return Pipeline.create()
      .thenCheck { state, _ -> state.currentElseStatement }
      .ifNotEquals(null) { it, _ ->
        it.thenDoAction { state, _ ->
          AddElseStatementIfStatementAction(
            file = state.currentFile!!,
            clazz = state.currentClass!!,
            ifStatement = state.currentIfStatement!!.copy(expression = action.value)
          )
        }
      }
      .ifEquals(null) { it, _ ->
        it.thenSetState { it.copy(currentIfStatement = null) }
          .thenDoAction { state, _ ->
            AddIfStatementAction(
              file = state.currentFile!!,
              clazz = state.currentClass!!,
              ifStatement = state.currentIfStatement!!.copy(expression = action.value)
            )
          }
      }
  }

  private fun parseIfStatement(tokenData: TokenData): Pipeline<*, *> {
    return Pipeline.create()
      .thenExpectToken { (tokens) -> tokens.consumeFirstIfType(TokenType.IF) }

      .thenExpectToken { (tokens) -> tokens.consumeFirstIfType(TokenType.OPENING_PAREN) }
      .orElseError("If statement missing opening parenthesis")

      .thenExpectToken { (tokens) -> tokens.consumeLastIfType(TokenType.OPENING_BRACE) }
      .orElseError("If statement missing opening brace")

      .thenExpectToken { (tokens) -> tokens.consumeLastIfType(TokenType.CLOSING_PAREN) }
      .orElseError("If statement missing closing parenthesis")

      .thenCheck { _, tokens -> tokens.isConsumed() }
      .ifTrueThenError("If statement missing expression")

      .thenSetState { it.copy(currentIfStatement = IfStatement(expression = Literal.NULL)) }

      .thenDoAction { _, _ -> IfStatementValueParseAction(tokenData = tokenData) }
  }
}
