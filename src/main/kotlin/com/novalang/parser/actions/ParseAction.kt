package com.novalang.parser.actions

import com.novalang.parser.TokenData

open class ParseAction(
  tokenData: TokenData
) : DispatcherAction(tokenData)
