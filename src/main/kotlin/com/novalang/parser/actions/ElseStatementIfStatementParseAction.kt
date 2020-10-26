package com.novalang.parser.actions

import com.novalang.parser.TokenData

class ElseStatementIfStatementParseAction(
  override val tokenData: TokenData
) : ParseAction(tokenData)
