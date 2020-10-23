package com.novalang.ast

import com.novalang.replace
import java.io.File as JavaFile

data class File(
  val file: JavaFile,
  val imports: List<Import> = emptyList(),
  val classes: List<Class> = emptyList(),
  override val id: Int = Node.counter++
) : Node {
  override fun equals(other: Any?): Boolean {
    return other is File && id == other.id
  }

  override fun hashCode(): Int {
    return id
  }
}
