package com.novalang.ast

data class Parameter(
  val name: String,
  val type: String?,
  val constant: Boolean,
  override val id: Int = Node.counter++
) : Node {
  override fun equals(other: Any?): Boolean {
    return other is Parameter && id == other.id
  }

  override fun hashCode(): Int {
    return id
  }
}
