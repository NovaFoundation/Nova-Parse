package com.novalang.parser

data class TokenList(
  val tokens: List<Token> = emptyList()
) {
  private val _unconsumed = mutableListOf(*tokens.toTypedArray())

  val unconsumed: List<Token>
    get() = _unconsumed

  fun consumeAll() {
    _unconsumed.clear()
  }

  fun consumeAllButLast(count: Int = 1) {
    val last = unconsumed.subList(unconsumed.size - count, unconsumed.size).toList()

    _unconsumed.clear()
    _unconsumed.addAll(last)
  }

  fun consumeFirst(): Token {
    return consumeFirst(1)[0]
  }

  fun consumeFirst(count: Int = 1): List<Token> {
    val consumed = unconsumed.take(count)
    val last = unconsumed.subList(count, unconsumed.size).toList()

    _unconsumed.clear()
    _unconsumed.addAll(last)

    return consumed
  }

  fun isNotConsumed(): Boolean {
    return !isConsumed()
  }

  fun isConsumed(): Boolean {
    return unconsumed.isEmpty()
  }
}
