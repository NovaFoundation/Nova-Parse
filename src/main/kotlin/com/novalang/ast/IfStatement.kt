package com.novalang.ast

data class IfStatement(
  val expression: Value,
  override val id: Int = Node.counter++
) : Node {
  override fun equals(other: Any?): Boolean {
    return other is IfStatement && id == other.id
  }

  override fun hashCode(): Int {
    return id
  }
}
