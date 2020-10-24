package com.novalang.parser

data class TokenData(
  val source: String = "",
  val currentTokens: TokenList = TokenList()
) {
  fun unconsumed(): TokenData {
    return copy(
      currentTokens = currentTokens.copy()
    )
  }

  fun consumeAll(): TokenData {
    return copy(
      currentTokens = currentTokens.clone().consumeAll()
    )
  }

  fun consumeAllButLast(count: Int = 1): TokenData {
    return copy(
      currentTokens = currentTokens.clone().consumeAllButLast(count)
    )
  }

  fun consumeFirst(): TokenData {
    val currentTokens = currentTokens.clone()
    currentTokens.consumeFirst()

    return copy(
      currentTokens = currentTokens
    )
  }

  fun consumeFirst(count: Int = 1): TokenData {
    val currentTokens = currentTokens.clone()
    currentTokens.consumeFirst(count)

    return copy(
      currentTokens = currentTokens
    )
  }

  fun isNotConsumed(): Boolean {
    return !isConsumed()
  }

  fun isConsumed(): Boolean {
    return currentTokens.isConsumed()
  }
}
