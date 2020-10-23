package com.novalang.parser.actions

import com.novalang.ast.Class
import com.novalang.ast.File
import com.novalang.ast.LocalDeclaration

class AddLocalDeclarationAction(
  val file: File,
  val clazz: Class,
  val localDeclaration: LocalDeclaration
) : DispatcherAction()
