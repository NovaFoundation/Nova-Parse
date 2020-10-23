package com.novalang.ast

data class Class(
  val name: String? = null,
  val fields: List<Field> = emptyList(),
  val functions: List<Function> = emptyList(),
  override val id: Int = Node.counter++
) : Node {
  override fun equals(other: Any?): Boolean {
    return other is Class && id == other.id
  }

  override fun hashCode(): Int {
    return id
  }
}
