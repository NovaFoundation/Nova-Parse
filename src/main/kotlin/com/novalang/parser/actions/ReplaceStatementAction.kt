package com.novalang.parser.actions

import com.novalang.ast.Node

class ReplaceStatementAction(
  val oldStatement: Node,
  val newStatement: Node,
) : DispatcherAction()
