package com.novalang.ast

data class Import(
  val location: String,
  override val id: Int = Node.counter++
) : Node {
  override fun equals(other: Any?): Boolean {
    return other is Import && id == other.id
  }

  override fun hashCode(): Int {
    return id
  }
}
