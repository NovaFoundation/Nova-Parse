package com.novalang.ast

interface Node {
  val id: Int

  companion object {
    var counter: Int = 1
  }
}

