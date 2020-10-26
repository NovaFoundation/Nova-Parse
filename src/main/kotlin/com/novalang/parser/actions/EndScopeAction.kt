package com.novalang.parser.actions

import com.novalang.ast.Scope
import com.novalang.parser.TokenData

class EndScopeAction(
  val scope: Scope,
  val tokenData: TokenData
) : DispatcherAction()
