package com.novalang.parser

import java.util.Stack

data class TokenList(
  val tokens: List<Token> = emptyList()
) {
  private var snapshots: Stack<List<Token>> = Stack()
  private val _unconsumed = mutableListOf(*tokens.toTypedArray())

  val unconsumed: List<Token>
    get() = _unconsumed

  fun createSnapshot(): TokenList {
    snapshots.push(unconsumed.toList())

    return this
  }

  fun restoreSnapshot(): TokenList {
    _unconsumed.clear()
    _unconsumed.addAll(snapshots.pop())

    return this
  }

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

  fun consumeAtIndexIfType(index: Int, type: TokenType): Token? {
    return consumeAtIndexIf(index) { it.type == type }
  }

  fun consumeAtIndexIf(index: Int, action: (Token) -> Boolean): Token? {
    return if (unconsumed.size > index && action(unconsumed[index])) {
      consumeAt(index)[0]
    } else {
      null
    }
  }

  fun consumeAtReverseIndexIfType(index: Int, type: TokenType): Token? {
    return consumeAtReverseIndexIf(index) { it.type == type }
  }

  fun consumeAtReverseIndexIf(index: Int, action: (Token) -> Boolean): Token? {
    return consumeAtIndexIf(unconsumed.size - index - 1, action)
  }

  fun consumeAt(index: Int, count: Int = 1) : List<Token> {
    val consumed = unconsumed.subList(index, index + count).toList()
    val first = unconsumed.subList(0, index).toList() + unconsumed.subList(index + count, unconsumed.size).toList()

    _unconsumed.clear()
    _unconsumed.addAll(first)

    return consumed
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
    return consumeAt(0, count)
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
    return consumeAt(unconsumed.size - count)
  }

  fun unconsumeLast(): Token {
    val last = tokens.last { it !in unconsumed }

    _unconsumed.add(last)

    return last
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
