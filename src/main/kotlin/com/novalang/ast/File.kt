package com.novalang.ast

import com.novalang.replace
import java.io.File as JavaFile

data class File(
  val file: JavaFile,
  val imports: List<Import> = emptyList(),
  val classes: List<Class> = emptyList(),
  override val id: Int = counter++
) : Node(id) {
  fun replaceClass(currentClass: Class, newClass: Class): File {
    return copy(
      classes = classes.replace(currentClass, newClass)
    )
  }

  override val children: List<Node>
    get() = classes

  override fun equals(other: Any?): Boolean {
    return super.equals(other)
  }

  override fun hashCode(): Int {
    return super.hashCode()
  }
}
