package com.novalang.parser.actions

import com.novalang.parser.TokenData
import java.io.File

class InitFileAction(
  val file: File,
  override val tokenData: TokenData = TokenData()
) : DispatcherAction(tokenData)
