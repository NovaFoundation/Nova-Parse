package com.novalang.parser.actions

import com.novalang.ast.Class
import com.novalang.ast.ElseStatement
import com.novalang.ast.File
import com.novalang.parser.TokenData

class AddElseStatementAction(
  val file: File,
  val clazz: Class,
  val elseStatement: ElseStatement,
  override val tokenData: TokenData = TokenData()
) : DispatcherAction(tokenData)
