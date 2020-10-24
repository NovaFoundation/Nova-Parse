package com.novalang.ast

data class Scope(
  val statements: List<Node> = emptyList(),
  override val id: Int = Node.counter++
) : Node {
  override fun equals(other: Any?): Boolean {
    return other is Scope && id == other.id
  }

  override fun hashCode(): Int {
    return id
  }
}
