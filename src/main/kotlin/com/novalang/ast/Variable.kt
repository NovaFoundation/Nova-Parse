package com.novalang.ast

data class Variable(
  val name: String,
  override val id: Int = Node.counter++
) : Value {
  override fun equals(other: Any?): Boolean {
    return other is Variable && id == other.id
  }

  override fun hashCode(): Int {
    return id
  }
}
