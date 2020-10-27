package com.novalang.parser.actions

import com.novalang.ast.Parameter
import com.novalang.parser.TokenData

class AddParameterAction(
  val parameter: Parameter,
  override val tokenData: TokenData = TokenData()
) : DispatcherAction(tokenData)
