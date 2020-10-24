package com.novalang.parser

import java.io.BufferedReader

private val SPLITTERS = setOf(
  ' ',
  '*',
  '/',
  '-',
  '+',
  '=',
  ':',
  '(',
  ')',
  '[',
  ']',
  '{',
  '}',
  '.',
  ',',
)

class Tokenizer(
  private val input: BufferedReader
) : Iterator<TokenData> {
  private var tokenData: TokenData = TokenData()
  private var nextLine: String? = null
  private var lineNumber: Int = 0
  // val tokenData: TokenData
  //   get() = _tokenData

  private fun nextLineWithContent(): String? {
    if (nextLine != null) {
      val value = nextLine

      nextLine = null

      return value
    }

    val line = input.readLine()
    lineNumber++

    val content = line?.trim()

    return when {
      content == null -> null
      content.startsWith("//") -> nextLineWithContent()
      !content.isBlank() -> line
      else -> nextLineWithContent()
    }
  }

  override fun next(): TokenData {
    val line = nextLineWithContent()
    val content = line!!.trim()
    val startWhitespace = line.indexOf(content[0])
    val tokens = mutableListOf<Token>()
    var prevIndex = 0
    var wasSymbol = false

    for (i in 1 until content.length) {
      if (SPLITTERS.contains(content[i])) {
        val str = content.substring(prevIndex, i)

        if (str.isNotBlank()) {
          tokens.add(
            Token(
              value = str,
              type = stringToTokenType(str),
              lineNumber = lineNumber,
              column = prevIndex + startWhitespace + 1
            )
          )
        }

        prevIndex = i
        wasSymbol = true
      } else if (i - 1 == prevIndex && wasSymbol) {
        val str = content.substring(prevIndex, i)

        if (str.isNotBlank()) {
          tokens.add(
            Token(
              value = str,
              type = stringToTokenType(str),
              lineNumber = lineNumber,
              column = prevIndex + startWhitespace + 1
            )
          )
        }

        prevIndex = i
        wasSymbol = false
      }
    }

    val lastStr = content.substring(prevIndex)

    tokens.add(
      Token(
        value = lastStr,
        type = stringToTokenType(lastStr),
        lineNumber = lineNumber,
        column = prevIndex + startWhitespace + 1
      )
    )

    tokenData = tokenData.copy(
      currentTokens = TokenList(compress(tokens)),
      source = line
    )

    return tokenData
  }

  private fun stringToTokenType(str: String): TokenType {
    return when (str) {
      "." -> TokenType.DOT
      " " -> TokenType.SPACE
      "{" -> TokenType.OPENING_BRACE
      "}" -> TokenType.CLOSING_BRACE
      "(" -> TokenType.OPENING_PAREN
      ")" -> TokenType.CLOSING_PAREN
      "+" -> TokenType.PLUS
      "-" -> TokenType.MINUS
      "=" -> TokenType.EQUALS
      ":" -> TokenType.COLON
      "*" -> TokenType.ASTERISK
      "/" -> TokenType.SLASH
      "|" -> TokenType.PIPE
      "&" -> TokenType.AMPERSAND
      "if" -> TokenType.IF
      "else" -> TokenType.ELSE
      "import" -> TokenType.IMPORT
      "class" -> TokenType.CLASS
      "let" -> TokenType.LET
      "var" -> TokenType.VAR
      "," -> TokenType.COMMA
      else -> TokenType.IDENTIFIER
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
