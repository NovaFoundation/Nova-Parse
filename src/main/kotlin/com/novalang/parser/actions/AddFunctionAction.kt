package com.novalang.parser.actions

import com.novalang.ast.Function
import com.novalang.parser.TokenData

class AddFunctionAction(
  val function: Function,
  override val tokenData: TokenData = TokenData()
) : DispatcherAction(tokenData)
