package com.novalang.ast

data class Scope(
  val localDeclarations: List<LocalDeclaration> = emptyList(),
  override val scope: Scope? = null,
  override val id: Int = Node.counter++
) : Scopeable {
  override fun setScope(scope: Scope?): Scopeable {
    return copy(scope = scope)
  }

  override fun equals(other: Any?): Boolean {
    return other is Scope && id == other.id
  }

  override fun hashCode(): Int {
    return id
  }
}
