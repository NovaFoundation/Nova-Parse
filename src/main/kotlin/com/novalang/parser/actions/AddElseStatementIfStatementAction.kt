package com.novalang.parser.actions

import com.novalang.ast.Class
import com.novalang.ast.File
import com.novalang.ast.IfStatement

class AddElseStatementIfStatementAction(
  val file: File,
  val clazz: Class,
  val ifStatement: IfStatement
) : DispatcherAction()
