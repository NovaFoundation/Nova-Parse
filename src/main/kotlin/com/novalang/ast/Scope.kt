package com.novalang.ast

data class Scope(
  val fields: List<Field> = emptyList(),
  override val id: Int = counter++
) : Node(id) {
  override val children: List<Node>
    get() = fields

  override fun equals(other: Any?): Boolean {
    return super.equals(other)
  }

  override fun hashCode(): Int {
    return super.hashCode()
  }
}
