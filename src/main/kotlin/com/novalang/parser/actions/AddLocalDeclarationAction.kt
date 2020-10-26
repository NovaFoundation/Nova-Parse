package com.novalang.parser.actions

import com.novalang.ast.Class
import com.novalang.ast.File
import com.novalang.ast.LocalDeclaration
import com.novalang.parser.TokenData

class AddLocalDeclarationAction(
  val file: File,
  val clazz: Class,
  val localDeclaration: LocalDeclaration,
  override val tokenData: TokenData = TokenData()
) : DispatcherAction(tokenData)
