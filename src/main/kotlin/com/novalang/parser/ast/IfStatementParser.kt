package com.novalang.parser.ast

import com.novalang.CompileError
import com.novalang.ast.Assignment
import com.novalang.ast.Literal
import com.novalang.ast.Variable
import com.novalang.parser.Dispatcher
import com.novalang.parser.State
import com.novalang.parser.TokenType
import com.novalang.parser.actions.AddAssignmentAction
import com.novalang.parser.actions.AddAssignmentValueAction
import com.novalang.parser.actions.AssignmentValueParseAction
import com.novalang.parser.actions.DispatcherAction
import com.novalang.parser.actions.ScopeParseAction

class IfStatementParser(dispatcher: Dispatcher) : Reducer(dispatcher) {
  override fun reduce(state: State, action: DispatcherAction): State {
    return when (action) {
      is ScopeParseAction -> parseAssignment(state, action)
      is AddAssignmentValueAction -> addAssignmentValue(state, action)
      else -> state
    }
  }

  private fun addAssignmentValue(state: State, action: AddAssignmentValueAction): State {
    return dispatcher.dispatchAndExecute(
      state.copy(
        currentAssignment = null
      ),
      AddAssignmentAction(
        file = state.currentFile!!,
        clazz = state.currentClass!!,
        assignment = state.currentAssignment!!.copy(assignmentValue = action.value)
      )
    )
  }

  private fun parseAssignment(state: State, action: ScopeParseAction): State {
    val tokens = action.tokenData.currentTokens

    if (tokens.unconsumed.size >= 2 && tokens.unconsumed[1].type == TokenType.EQUALS) {
      val variableNameToken = tokens.consumeFirst()

      if (variableNameToken.type != TokenType.IDENTIFIER) {
        tokens.consumeAll()

        return state.copy(
          errors = state.errors + CompileError(
            message = "Invalid variable name for assignment",
            tokenData = action.tokenData.unconsumed()
          )
        )
      }

      // equals sign
      tokens.consumeFirst()

      val assignment = Assignment(
        variable = Variable(variableNameToken.value),
        assignmentValue = Literal.NULL
      )

      return dispatcher.dispatchAndExecute(
        state = state.copy(currentAssignment = assignment),
        action = AssignmentValueParseAction(tokenData = action.tokenData)
      )
    }

    return state
  }
}
