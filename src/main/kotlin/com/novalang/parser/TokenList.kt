package com.novalang.parser

data class TokenList(
  val tokens: List<Token> = emptyList()
) {
  private val _unconsumed = mutableListOf(*tokens.toTypedArray())

  val unconsumed: List<Token>
    get() = _unconsumed

  fun consumeAll(): TokenList {
    _unconsumed.clear()

    return this
  }

  fun consumeAllButLast(count: Int = 1): TokenList {
    val last = unconsumed.subList(unconsumed.size - count, unconsumed.size).toList()

    _unconsumed.clear()
    _unconsumed.addAll(last)

    return this
  }

  fun consumeFirst(): Token {
    return consumeFirst(1)[0]
  }

  fun consumeFirstIfType(type: TokenType): Token? {
    return consumeFirstIf { it.type == type }
  }

  fun consumeFirstIf(action: (Token) -> Boolean): Token? {
    return if (action(unconsumed.first())) {
      consumeFirst(1)[0]
    } else {
      null
    }
  }

  fun consumeFirst(count: Int = 1): List<Token> {
    val consumed = unconsumed.take(count)
    val last = unconsumed.subList(count, unconsumed.size).toList()

    _unconsumed.clear()
    _unconsumed.addAll(last)

    return consumed
  }

  fun consumeLastIfType(type: TokenType): Token? {
    return consumeLastIf { it.type == type }
  }

  fun consumeLastIf(action: (Token) -> Boolean): Token? {
    return if (action(unconsumed.last())) {
      consumeLast(1)[0]
    } else {
      null
    }
  }

  fun consumeLast(count: Int = 1) : List<Token> {
    val consumed = unconsumed.takeLast(count)
    val first = unconsumed.subList(0, unconsumed.size - count).toList()

    _unconsumed.clear()
    _unconsumed.addAll(first)

    return consumed
  }

  fun isNotConsumed(): Boolean {
    return !isConsumed()
  }

  fun isConsumed(): Boolean {
    return unconsumed.isEmpty()
  }

  fun clone(): TokenList {
    val clone = copy()

    clone._unconsumed.clear()
    clone._unconsumed.addAll(_unconsumed)

    return clone
  }
}
