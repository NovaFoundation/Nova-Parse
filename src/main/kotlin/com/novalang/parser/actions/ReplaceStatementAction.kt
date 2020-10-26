package com.novalang.parser.actions

import com.novalang.ast.Node
import com.novalang.parser.TokenData

class ReplaceStatementAction(
  val oldStatement: Node,
  val newStatement: Node,
  override val tokenData: TokenData = TokenData()
) : DispatcherAction(tokenData)
