package com.novalang.parser.actions

import com.novalang.ast.Class
import com.novalang.ast.File
import com.novalang.ast.Value
import com.novalang.parser.TokenData

class AddAssignmentValueAction(
  val file: File,
  val clazz: Class,
  val value: Value,
  override val tokenData: TokenData = TokenData()
) : DispatcherAction(tokenData)
