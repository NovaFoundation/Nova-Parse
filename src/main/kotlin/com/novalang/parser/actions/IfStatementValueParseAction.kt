package com.novalang.parser.actions

import com.novalang.parser.TokenData

class IfStatementValueParseAction(
  override val tokenData: TokenData
) : ParseAction(tokenData)
