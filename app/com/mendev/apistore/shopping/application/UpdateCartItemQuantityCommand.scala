package com.mendev.apistore.shopping.application

final case class UpdateCartItemQuantityCommand(productId: Long, quantity: Int)
