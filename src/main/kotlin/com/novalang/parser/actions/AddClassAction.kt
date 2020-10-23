package com.novalang.parser.actions

import com.novalang.ast.Class
import com.novalang.ast.File

class AddClassAction(
  val file: File,
  val newClass: Class,
) : DispatcherAction()
