package com.mendev.apistore.shopping.application

final case class AddToCartCommand(productId: Long, quantity: Int)
