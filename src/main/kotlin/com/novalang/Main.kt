package com.novalang

import com.novalang.parser.Dispatcher
import com.novalang.parser.Parser
import com.novalang.parser.State
import com.novalang.parser.ast.FieldParser
import com.novalang.parser.ast.FileParser
import com.novalang.parser.ast.ImportParser
import com.novalang.parser.ast.ClassParser
import com.novalang.parser.ast.FunctionParser
import com.novalang.parser.ast.ScopeParser
import java.io.File

fun main(args: Array<String>) {
  val dispatcher = Dispatcher()

  dispatcher
    .register(FileParser(dispatcher))
    .register(FieldParser(dispatcher))
    .register(FunctionParser(dispatcher))
    .register(ImportParser(dispatcher))
    .register(ClassParser(dispatcher))
    .register(ScopeParser(dispatcher))

  val parser = Parser(dispatcher)

  val file = File(args[0])

  var state = parser.parseFile(State(), file)

  var unparsedFiles = getUnparsedFiles(state)

  do {
    state = unparsedFiles.fold(state, parser::parseFile)

    unparsedFiles = getUnparsedFiles(state)
  } while (unparsedFiles.isNotEmpty())

  println(state)

  state.errors.forEach { System.err.println("Error: \"${it.message}\"\n\t${it.source.trim()}") }
}

private fun getUnparsedFiles(state: State): List<File> {
  return state.imports
    .map { File("${it.location}.nova") }
    .filter { state.parsedFiles.none { file -> file.canonicalPath == it.canonicalPath } }
}
