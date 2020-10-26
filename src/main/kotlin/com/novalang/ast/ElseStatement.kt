package com.novalang.ast

data class ElseStatement(
  val ifStatement: IfStatement? = null,
  val scope: Scope? = null,
  override val id: Int = Node.counter++
) : Node {
  override fun equals(other: Any?): Boolean {
    return other is ElseStatement && id == other.id
  }

  override fun hashCode(): Int {
    return id
  }
}
