package com.novalang.ast

abstract class Node(
  open val id: Int
) {
  abstract val children: List<Node>

  override fun hashCode(): Int {
    return id
  }

  override fun equals(other: Any?): Boolean {
    return other is Node && id == other.id
  }

  companion object {
    var counter: Int = 1
  }
}
