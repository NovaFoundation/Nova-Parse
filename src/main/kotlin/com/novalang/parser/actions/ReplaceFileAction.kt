package com.novalang.parser.actions

import com.novalang.ast.File
import com.novalang.parser.TokenData

class ReplaceFileAction(
  val oldFile: File,
  val newFile: File,
  override val tokenData: TokenData = TokenData()
) : DispatcherAction(tokenData)
