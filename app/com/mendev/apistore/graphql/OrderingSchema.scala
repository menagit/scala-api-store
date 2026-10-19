package com.mendev.apistore.graphql

import com.mendev.apistore.ordering.application.CheckoutUseCase
import com.mendev.apistore.ordering.domain.{Order, OrderItem}
import com.mendev.apistore.shared.security.Client
import sangria.schema.{BigDecimalType, Field, IntType, ListType, LongType, ObjectType, StringType, fields}

import scala.concurrent.ExecutionContext

class OrderingSchema(checkout: CheckoutUseCase)(implicit ec: ExecutionContext) {

  private val OrderItemType: ObjectType[GraphQLContext, OrderItem] = ObjectType(
    "OrderItem",
    fields[GraphQLContext, OrderItem](
      Field("productId", LongType, resolve = _.value.productId),
      Field("productName", StringType, resolve = _.value.productName),
      Field("unitPrice", BigDecimalType, resolve = _.value.unitPrice),
      Field("discountPercent", BigDecimalType, resolve = _.value.discountPercent),
      Field("discountedUnitPrice", BigDecimalType, resolve = _.value.discountedUnitPrice),
      Field("quantity", IntType, resolve = _.value.quantity),
      Field("lineTotal", BigDecimalType, resolve = _.value.lineTotal)
    )
  )

  private val OrderType: ObjectType[GraphQLContext, Order] = ObjectType(
    "Order",
    fields[GraphQLContext, Order](
      Field("id", StringType, resolve = _.value.publicId.toString),
      Field("status", StringType, resolve = _.value.status.toString.toUpperCase),
      Field("total", BigDecimalType, resolve = _.value.total),
      Field("createdAt", StringType, resolve = _.value.createdAt.toString),
      Field("items", ListType(OrderItemType), resolve = _.value.items)
    )
  )

  val mutationFields: List[Field[GraphQLContext, Unit]] = List(
    Field(
      "checkout",
      OrderType,
      resolve = c => RoleGuard.require(c.ctx.actor, Client)(AppErrorException.orFail(checkout.execute(c.ctx.actor)))
    )
  )
}
