package com.novalang.parser.actions

import com.novalang.ast.Class
import com.novalang.ast.Field
import com.novalang.ast.File

class AddFieldAction(
  val file: File,
  val clazz: Class,
  val field: Field
) : DispatcherAction()
