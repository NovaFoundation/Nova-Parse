package com.novalang.parser

data class TokenData(
  val source: String = "",
  val currentTokens: TokenList = TokenList()
)
