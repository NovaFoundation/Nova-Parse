package com.novalang.parser.actions

import com.novalang.parser.TokenData

class FileParseAction(
  override val tokenData: TokenData
) : ParseAction(tokenData)
