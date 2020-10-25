package com.novalang.ast

enum class LiteralType {
  BOOLEAN,
  BYTE,
  SHORT,
  INT,
  LONG,
  NULL
}

data class Literal(
  val value: String,
  val type: LiteralType,
  override val id: Int = Node.counter++
) : Value {
  override fun equals(other: Any?): Boolean {
    return other is Literal && id == other.id
  }

  override fun hashCode(): Int {
    return id
  }

  companion object {
    val NULL = Literal("null", LiteralType.NULL)
  }
}
