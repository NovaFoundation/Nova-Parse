package com.novalang.parser.actions

import com.novalang.ast.Function
import com.novalang.parser.TokenData

class ParameterParseAction(
  override val tokenData: TokenData,
  val function: Function
) : ParseAction(tokenData)
