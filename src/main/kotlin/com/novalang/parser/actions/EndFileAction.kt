package com.novalang.parser.actions

import com.novalang.parser.TokenData

class EndFileAction(
  override val tokenData: TokenData = TokenData()
): DispatcherAction(tokenData)
