package com.novalang.parser.actions

import com.novalang.parser.TokenData

open class ParseAction(
  open val tokenData: TokenData
) : DispatcherAction()
