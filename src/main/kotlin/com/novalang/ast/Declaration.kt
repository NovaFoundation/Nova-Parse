package com.novalang.ast

open class Declaration(
  open val name: String,
  open val type: String?,
  open val constant: Boolean,
  override val id: Int
) : Node
