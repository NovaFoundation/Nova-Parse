package com.novalang.parser.actions

import com.novalang.ast.Class
import com.novalang.ast.File
import com.novalang.ast.Function
import com.novalang.ast.Parameter
import com.novalang.parser.TokenData

class AddParameterAction(
  val file: File,
  val clazz: Class,
  val function: Function,
  val parameter: Parameter,
  override val tokenData: TokenData = TokenData()
) : DispatcherAction(tokenData)
