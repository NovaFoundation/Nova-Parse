package com.novalang.parser

data class TokenData(
  val tokens: TokenList = TokenList(),
  val source: String = ""
) {
  fun createSnapshot(): TokenData {
    tokens.createSnapshot()

    return this
  }

  fun restoreSnapshot(): TokenData {
    tokens.restoreSnapshot()

    return this
  }

  fun dropSnapshot(): TokenData {
    tokens.dropSnapshot()

    return this
  }

  fun unconsumed(): TokenData {
    return copy(
      tokens = tokens.copy()
    )
  }

  fun consumeAll(): TokenData {
    return copy(
      tokens = tokens.clone().consumeAll()
    )
  }

  fun consumeAllButLast(count: Int = 1): TokenData {
    return copy(
      tokens = tokens.clone().consumeAllButLast(count)
    )
  }

  fun consumeFirst(): TokenData {
    val currentTokens = tokens.clone()
    currentTokens.consumeFirst()

    return copy(
      tokens = currentTokens
    )
  }

  fun consumeFirst(count: Int = 1): TokenData {
    val currentTokens = tokens.clone()
    currentTokens.consumeFirst(count)

    return copy(
      tokens = currentTokens
    )
  }

  fun isNotConsumed(): Boolean {
    return !isConsumed()
  }

  fun isConsumed(): Boolean {
    return tokens.isConsumed()
  }
}
