package com.novalang.parser.actions

import com.novalang.ast.Class
import com.novalang.ast.Field
import com.novalang.ast.File
import com.novalang.parser.TokenData

class AddFieldAction(
  val file: File,
  val clazz: Class,
  val field: Field,
  override val tokenData: TokenData = TokenData()
) : DispatcherAction(tokenData)
