package com.novalang.ast

interface Scopeable : Node {
  val scope: Scope?

  fun setScope(scope: Scope?): Scopeable
}
