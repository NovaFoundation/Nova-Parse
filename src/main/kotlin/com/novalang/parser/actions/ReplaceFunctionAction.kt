package com.novalang.parser.actions

import com.novalang.ast.Class
import com.novalang.ast.File
import com.novalang.ast.Function

class ReplaceFunctionAction(
  val file: File,
  val clazz: Class,
  val oldFunction: Function,
  val newFunction: Function,
) : DispatcherAction()
