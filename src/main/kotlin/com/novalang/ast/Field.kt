package com.novalang.ast

data class Field(
  override val name: String,
  override val type: String?,
  override val constant: Boolean,
  override val id: Int = Node.counter++
) : Declaration(name, type, constant, id) {
  override fun equals(other: Any?): Boolean {
    return other is Field && id == other.id
  }

  override fun hashCode(): Int {
    return id
  }
}
