package com.novalang.parser.actions

import com.novalang.ast.Class
import com.novalang.ast.ElseStatement
import com.novalang.ast.File

class AddElseStatementAction(
  val file: File,
  val clazz: Class,
  val elseStatement: ElseStatement
) : DispatcherAction()
