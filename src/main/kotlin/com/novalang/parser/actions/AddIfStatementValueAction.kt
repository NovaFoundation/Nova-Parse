package com.novalang.parser.actions

import com.novalang.ast.Class
import com.novalang.ast.File
import com.novalang.ast.Value

class AddIfStatementValueAction(
  val file: File,
  val clazz: Class,
  val value: Value
) : DispatcherAction()
