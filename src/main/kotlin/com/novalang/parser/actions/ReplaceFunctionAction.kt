package com.novalang.parser.actions

import com.novalang.ast.Class
import com.novalang.ast.File
import com.novalang.ast.Function
import com.novalang.parser.TokenData

class ReplaceFunctionAction(
  val oldFunction: Function,
  val newFunction: Function,
  override val tokenData: TokenData = TokenData()
) : DispatcherAction(tokenData)
