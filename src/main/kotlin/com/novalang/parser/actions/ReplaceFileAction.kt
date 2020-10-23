package com.novalang.parser.actions

import com.novalang.ast.File

class ReplaceFileAction(
  val oldFile: File,
  val newFile: File
) : DispatcherAction()
