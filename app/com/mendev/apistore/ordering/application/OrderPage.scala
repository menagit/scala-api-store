package com.mendev.apistore.ordering.application

import com.mendev.apistore.ordering.domain.Order

final case class OrderPage(items: List[Order], page: Int, size: Int, totalItems: Long, totalPages: Long)
