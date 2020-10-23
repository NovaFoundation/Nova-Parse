package com.novalang.parser

import java.io.BufferedReader

private val SPLITTERS = setOf(
  ' ',
  '*',
  '/',
  '-',
  '+',
  '=',
  '(',
  ')',
  '[',
  ']',
  '{',
  '}',
  '.',
  // '"',
)

class Tokenizer(
  private val input: BufferedReader
) : Iterator<TokenData> {
  private var tokenData: TokenData = TokenData()
  private var nextLine: String? = null

  // val tokenData: TokenData
  //   get() = _tokenData

  private fun nextLineWithContent(): String? {
    if (nextLine != null) {
      val value = nextLine

      nextLine = null

      return value
    }

    val line = input.readLine()

    return when {
      line == null -> null
      line.trim().isNotEmpty() -> line
      else -> nextLineWithContent()
    }
  }

  override fun next(): TokenData {
    val line = nextLineWithContent()
    val content = line!!.trim()
    val tokens = mutableListOf<Token>()
    var prevIndex = 0
    var wasSymbol = false

    for (i in 1 until content.length) {
      if (SPLITTERS.contains(content[i])) {
        val str = content.substring(prevIndex, i)

        if (str.isNotBlank()) {
          tokens.add(stringToToken(str))
        }

        prevIndex = i
        wasSymbol = true
      } else if (i - 1 == prevIndex && wasSymbol) {
        val str = content.substring(prevIndex, i)

        if (str.isNotBlank()) {
          tokens.add(stringToToken(str))
        }

        prevIndex = i
        wasSymbol = false
      }
    }

    tokens.add(stringToToken(content.substring(prevIndex)))

    tokenData = tokenData.copy(
      currentTokens = TokenList(compress(tokens)),
      source = line
    )

    return tokenData
  }

  private fun stringToToken(str: String): Token {
    return when (str) {
      "." -> Token(value = str, type = TokenType.DOT)
      " " -> Token(value = str, type = TokenType.SPACE)
      "{" -> Token(value = str, type = TokenType.OPENING_BRACE)
      "}" -> Token(value = str, type = TokenType.CLOSING_BRACE)
      "(" -> Token(value = str, type = TokenType.OPENING_PAREN)
      ")" -> Token(value = str, type = TokenType.CLOSING_PAREN)
      "+" -> Token(value = str, type = TokenType.PLUS)
      "-" -> Token(value = str, type = TokenType.MINUS)
      "=" -> Token(value = str, type = TokenType.EQUALS)
      ":" -> Token(value = str, type = TokenType.COLON)
      "*" -> Token(value = str, type = TokenType.ASTERISK)
      "/" -> Token(value = str, type = TokenType.SLASH)
      "|" -> Token(value = str, type = TokenType.PIPE)
      "&" -> Token(value = str, type = TokenType.AMPERSAND)
      "import" -> Token(value = str, type = TokenType.IMPORT)
      "class" -> Token(value = str, type = TokenType.CLASS)
      "let" -> Token(value = str, type = TokenType.LET)
      "var" -> Token(value = str, type = TokenType.VAR)
      "," -> Token(value = str, type = TokenType.COMMA)
      else -> Token(value = str, type = TokenType.IDENTIFIER)
    }
  }

  private fun compress(tokens: List<Token>): List<Token> {
    val compressed = mutableListOf<Token>()

    compressed.addAll(tokens)

    return compressed
  }

  override fun hasNext(): Boolean {
    if (nextLine == null) {
      nextLine = nextLineWithContent()
    }

    return nextLine != null
  }
}
