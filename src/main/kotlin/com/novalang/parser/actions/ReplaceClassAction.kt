package com.novalang.parser.actions

import com.novalang.ast.Class
import com.novalang.ast.File
import com.novalang.parser.TokenData

class ReplaceClassAction(
  val oldClass: Class,
  val newClass: Class,
  override val tokenData: TokenData = TokenData()
) : DispatcherAction(tokenData)
