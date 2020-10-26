package com.novalang.parser.actions

import com.novalang.ast.Assignment
import com.novalang.ast.Class
import com.novalang.ast.File
import com.novalang.parser.TokenData

class AddAssignmentAction(
  val file: File,
  val clazz: Class,
  val assignment: Assignment,
  override val tokenData: TokenData = TokenData()
) : DispatcherAction(tokenData)
