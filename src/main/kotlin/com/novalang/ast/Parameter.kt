package com.novalang.ast

data class Parameter(
  val name: String,
  val type: String?,
  val constant: Boolean,
  override val id: Int = counter++
) : Node(id) {
  override fun equals(other: Any?): Boolean {
    return super.equals(other)
  }

  override fun hashCode(): Int {
    return super.hashCode()
  }
}
