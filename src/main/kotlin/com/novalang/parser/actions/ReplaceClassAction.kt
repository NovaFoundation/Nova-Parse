package com.novalang.parser.actions

import com.novalang.ast.Class
import com.novalang.ast.File

class ReplaceClassAction(
  val file: File,
  val oldClass: Class,
  val newClass: Class,
) : DispatcherAction()
