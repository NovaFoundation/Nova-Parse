package com.novalang.ast

data class ScopeBlock(
  val scope: Scope? = null,
  override val id: Int = Node.counter++
) : Node {
  override fun equals(other: Any?): Boolean {
    return other is ScopeBlock && id == other.id
  }

  override fun hashCode(): Int {
    return id
  }
}
