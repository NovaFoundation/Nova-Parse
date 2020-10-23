package com.novalang.ast

data class Function(
  val name: String,
  val parameters: List<Parameter> = emptyList(),
  override val id: Int = counter++
) : Node(id) {
  override fun equals(other: Any?): Boolean {
    return super.equals(other)
  }

  override fun hashCode(): Int {
    return super.hashCode()
  }
}
