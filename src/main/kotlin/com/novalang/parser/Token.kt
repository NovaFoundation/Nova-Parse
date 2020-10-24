package com.novalang.parser

data class Token(
  val type: TokenType,
  val value: String,
  val lineNumber: Int,
  val column: Int
)
