package com.mendev.apistore.shopping.application

final case class CartView(lines: List[CartLine], total: BigDecimal)
