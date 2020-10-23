package com.novalang.ast

data class Class(
  val name: String? = null,
  val scope: Scope? = null,
  val fields: List<Field> = emptyList(),
  val functions: List<Function> = emptyList(),
  override val id: Int = counter++
) : Node(id) {
  override fun equals(other: Any?): Boolean {
    return super.equals(other)
  }

  override fun hashCode(): Int {
    return super.hashCode()
  }
}
