package com.novalang

import com.novalang.parser.TokenData

data class CompileError(
  val message: String,
  val source: String? = null,
  val lineNumber: Int? = null,
  val columnNumber: Int? = null
) {
  constructor(
    message: String,
    tokenData: TokenData
  ) : this(message, sourceFromTokenData(tokenData), sourceLineNumber(tokenData), sourceStart(tokenData) + 1)

  override fun toString(): String {
    return if (source != null && lineNumber != null && columnNumber != null) {
      "Error[${lineNumber},${columnNumber}]: \"${message}\"\n\t'${source}'"
    } else {
      "Error: \"$message\""
    }
  }
}

private fun sourceLineNumber(tokenData: TokenData): Int {
  return tokenData.tokens.tokens.first().lineNumber
}

private fun sourceFromTokenData(tokenData: TokenData): String {
  return tokenData.source.substring(sourceStart(tokenData), sourceEnd(tokenData))
}

private fun sourceStart(tokenData: TokenData): Int {
  val firstToken = tokenData.tokens.unconsumed.firstOrNull() ?: return 0

  return firstToken.column - 1
}

private fun sourceEnd(tokenData: TokenData): Int {
  val lastToken = tokenData.tokens.unconsumed.lastOrNull() ?: return 0

  return lastToken.column + lastToken.value.length - 1
}
