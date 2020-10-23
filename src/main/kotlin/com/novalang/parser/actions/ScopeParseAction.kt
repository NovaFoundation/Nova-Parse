package com.novalang.parser.actions

import com.novalang.parser.TokenData

class ScopeParseAction(
  override val tokenData: TokenData
) : ParseAction(tokenData)
