package com.novalang.ast

data class Class(
  val name: String? = null,
  val scope: Scope? = null,
  val functions: List<Function> = emptyList(),
  override val id: Int = counter++
) : Node(id) {
  override val children: List<Node>
    get() = listOfNotNull(scope) + functions

  override fun equals(other: Any?): Boolean {
    return super.equals(other)
  }

  override fun hashCode(): Int {
    return super.hashCode()
  }
}
