package com.novalang.ast

data class Assignment(
  val variable: Variable,
  val assignmentValue: Value,
  override val id: Int = Node.counter++
) : Node {
  override fun equals(other: Any?): Boolean {
    return other is Assignment && id == other.id
  }

  override fun hashCode(): Int {
    return id
  }
}
