package com.novalang.parser.actions

import com.novalang.ast.Class
import com.novalang.ast.File
import com.novalang.ast.Scope
import com.novalang.ast.Scopeable

class ReplaceScopeAction(
  val file: File,
  val clazz: Class,
  val scopeable: Scopeable,
  val oldScope: Scope,
  val newScope: Scope,
) : DispatcherAction()
