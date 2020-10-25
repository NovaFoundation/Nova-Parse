package com.novalang.parser

import com.novalang.CompileError
import com.novalang.ast.Assignment
import com.novalang.ast.Class
import com.novalang.ast.File
import com.novalang.ast.Function
import com.novalang.ast.IfStatement
import com.novalang.ast.Import
import com.novalang.ast.Scope
import java.io.File as JavaFile

data class State(
  val files: List<File> = emptyList(),
  val currentFile: File? = null,
  val currentClass: Class? = null,
  val currentFunction: Function? = null,
  val currentAssignment: Assignment? = null,
  val currentIfStatement: IfStatement? = null,
  val errors: List<CompileError> = emptyList(),
  val imports: Set<Import> = emptySet(),
  val parsedFiles: Set<JavaFile> = emptySet(),
  val scopes: List<Scope> = emptyList()
)
