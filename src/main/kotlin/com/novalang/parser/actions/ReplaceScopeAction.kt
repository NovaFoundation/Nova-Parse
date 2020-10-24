package com.novalang.parser.actions

import com.novalang.ast.Class
import com.novalang.ast.File
import com.novalang.ast.Scope

class ReplaceScopeAction(
  val file: File,
  val clazz: Class,
  val oldScope: Scope,
  val newScope: Scope,
) : DispatcherAction()
