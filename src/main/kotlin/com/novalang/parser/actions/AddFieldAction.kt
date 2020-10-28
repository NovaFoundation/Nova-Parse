package com.novalang.parser.actions

import com.novalang.ast.Field
import com.novalang.parser.TokenData

class AddFieldAction(
  val field: Field,
  override val tokenData: TokenData = TokenData()
) : DispatcherAction(tokenData)
