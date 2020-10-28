package com.novalang.parser.ast

import com.novalang.ast.Assignment
import com.novalang.ast.Literal
import com.novalang.ast.Variable
import com.novalang.parser.Dispatcher
import com.novalang.parser.Pipeline
import com.novalang.parser.State
import com.novalang.parser.Token
import com.novalang.parser.TokenType
import com.novalang.parser.actions.AddAssignmentAction
import com.novalang.parser.actions.AddAssignmentValueAction
import com.novalang.parser.actions.AssignmentValueParseAction
import com.novalang.parser.actions.DispatcherAction
import com.novalang.parser.actions.ScopeParseAction

class AssignmentParser(dispatcher: Dispatcher) : Reducer(dispatcher) {
  override fun reduce(state: State, action: DispatcherAction): State {
    return when (action) {
      is ScopeParseAction -> parseAssignment(action).run(dispatcher, state, action.tokenData)
      is AddAssignmentValueAction -> addAssignmentValue(action).run(dispatcher, state, action.tokenData)
      else -> state
    }
  }

  private fun addAssignmentValue(action: AddAssignmentValueAction): Pipeline<*, *> {
    return Pipeline.create()
      .thenDoAction { state, _ ->
        AddAssignmentAction(
          file = state.currentFile!!,
          clazz = state.currentClass!!,
          assignment = state.currentAssignment!!.copy(assignmentValue = action.value)
        )
      }

      .thenSetState { state -> state.copy(currentAssignment = null) }
  }

  private fun parseAssignment(action: ScopeParseAction): Pipeline<*, *> {
    lateinit var variableNameToken: Token

    return Pipeline.create()
      .thenExpectToken { (tokens) -> tokens.consumeAtIndexIfType(1, TokenType.EQUALS) }

      .thenExpectToken { (tokens) -> tokens.consumeFirstIfType(TokenType.IDENTIFIER) }
      .orElseError("Invalid variable name for assignment")
      .thenDoWithResponse { variableNameToken = it.token!! }

      .thenSetState { state ->
        state.copy(
          currentAssignment = Assignment(
            variable = Variable(variableNameToken.value),
            assignmentValue = Literal.NULL
          )
        )
      }

      .thenDoAction { _, _ -> AssignmentValueParseAction(tokenData = action.tokenData) }
  }
}
