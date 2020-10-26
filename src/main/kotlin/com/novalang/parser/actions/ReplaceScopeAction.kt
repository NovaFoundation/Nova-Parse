package com.novalang.parser.actions

import com.novalang.ast.Class
import com.novalang.ast.File
import com.novalang.ast.Scope
import com.novalang.parser.TokenData

class ReplaceScopeAction(
  val file: File,
  val clazz: Class,
  val oldScope: Scope,
  val newScope: Scope,
  override val tokenData: TokenData = TokenData()
) : DispatcherAction(tokenData)
