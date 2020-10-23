package com.novalang.parser.actions

import com.novalang.parser.TokenData

class ClassParseAction(
  override val tokenData: TokenData
) : ParseAction(tokenData)
