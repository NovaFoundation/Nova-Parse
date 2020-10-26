package com.novalang.parser.actions

import com.novalang.ast.Class
import com.novalang.ast.File
import com.novalang.ast.IfStatement
import com.novalang.parser.TokenData

class AddIfStatementAction(
  val file: File,
  val clazz: Class,
  val ifStatement: IfStatement,
  override val tokenData: TokenData = TokenData()
) : DispatcherAction(tokenData)
