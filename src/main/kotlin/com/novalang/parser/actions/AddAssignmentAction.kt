package com.novalang.parser.actions

import com.novalang.ast.Assignment
import com.novalang.ast.Class
import com.novalang.ast.File

class AddAssignmentAction(
  val file: File,
  val clazz: Class,
  val assignment: Assignment
) : DispatcherAction()
