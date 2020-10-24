package com.novalang.parser.actions

import com.novalang.parser.TokenData

class AssignmentValueParseAction(
  override val tokenData: TokenData
) : ParseAction(tokenData)
