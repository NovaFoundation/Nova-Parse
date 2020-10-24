package com.novalang.ast

data class Literal(
  val value: String,
  override val id: Int = Node.counter++
) : Value {
  override fun equals(other: Any?): Boolean {
    return other is Literal && id == other.id
  }

  override fun hashCode(): Int {
    return id
  }

  companion object {
    val NULL = Literal("null")
  }
}
