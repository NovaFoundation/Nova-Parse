package com.novalang.ast

data class Field(
  val name: String,
  val constant: Boolean,
  override val id: Int = counter++
) : Node(id) {
  override val children: List<Node>
    get() = emptyList()

  override fun equals(other: Any?): Boolean {
    return super.equals(other)
  }

  override fun hashCode(): Int {
    return super.hashCode()
  }
}
