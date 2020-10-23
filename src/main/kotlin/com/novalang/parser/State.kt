package com.novalang.parser

import com.novalang.CompileError
import com.novalang.ast.Class
import com.novalang.ast.File
import com.novalang.ast.Function
import com.novalang.ast.Import
import com.novalang.ast.Scope
import com.novalang.ast.Scopeable
import java.io.File as JavaFile

data class State(
  val files: List<File> = emptyList(),
  val currentFile: File? = null,
  val currentClass: Class? = null,
  val currentFunction: Function? = null,
  val errors: List<CompileError> = emptyList(),
  val imports: Set<Import> = emptySet(),
  val parsedFiles: Set<JavaFile> = emptySet(),
  val scopes: List<Scopeable> = emptyList()
)
