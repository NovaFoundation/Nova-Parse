package com.novalang.parser.actions

import com.novalang.ast.Class
import com.novalang.ast.File
import com.novalang.ast.Function

class AddFunctionAction(
  val file: File,
  val clazz: Class,
  val function: Function
) : DispatcherAction()
