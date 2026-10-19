package com.mendev.apistore.graphql

import com.mendev.apistore.ordering.application.{ChangeOrderStatusUseCase, CheckoutUseCase, GetMyOrderUseCase, ListMyOrdersQuery, ListMyOrdersUseCase, ListOrdersQuery, ListOrdersUseCase, OrderPage}
import com.mendev.apistore.ordering.domain.OrderStatus.{Cancelled, Delivered, Shipped}
import com.mendev.apistore.ordering.domain.{Order, OrderItem, OrderStatus}
import com.mendev.apistore.shared.security.{Actor, Client, Manager}
import sangria.schema.{Argument, BigDecimalType, Field, IntType, ListType, LongType, ObjectType, OptionInputType, OptionType, StringType, fields}

import scala.concurrent.{ExecutionContext, Future}

class OrderingSchema(
  checkout: CheckoutUseCase,
  listMyOrders: ListMyOrdersUseCase,
  getMyOrder: GetMyOrderUseCase,
  listOrders: ListOrdersUseCase,
  changeOrderStatus: ChangeOrderStatusUseCase
)(implicit ec: ExecutionContext) {

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
      Field("trackingNumber", OptionType(StringType), resolve = _.value.trackingNumber.map(_.value)),
      Field("estimatedDelivery", OptionType(StringType), resolve = _.value.estimatedDelivery.map(_.toString)),
      Field("createdAt", StringType, resolve = _.value.createdAt.toString),
      Field("updatedAt", StringType, resolve = _.value.updatedAt.toString),
      Field("items", ListType(OrderItemType), resolve = _.value.items)
    )
  )

  private val OrderPageType: ObjectType[GraphQLContext, OrderPage] = ObjectType(
    "OrderPage",
    fields[GraphQLContext, OrderPage](
      Field("items", ListType(OrderType), resolve = _.value.items),
      Field("page", IntType, resolve = _.value.page),
      Field("size", IntType, resolve = _.value.size),
      Field("totalItems", LongType, resolve = _.value.totalItems),
      Field("totalPages", LongType, resolve = _.value.totalPages)
    )
  )

  private val PageArg = Argument("page", OptionInputType(IntType), defaultValue = 1)
  private val SizeArg = Argument("size", OptionInputType(IntType), defaultValue = 20)
  private val IdArg   = Argument("id", StringType)

  private val TrackingNumberArg    = Argument("trackingNumber", StringType)
  private val EstimatedDeliveryArg = Argument("estimatedDelivery", StringType)

  val queryFields: List[Field[GraphQLContext, Unit]] = List(
    Field(
      "myOrders",
      OrderPageType,
      arguments = List(PageArg, SizeArg),
      resolve = c =>
        RoleGuard.require(c.ctx.actor, Client) {
          AppErrorException.orFail(listMyOrders.execute(c.ctx.actor, ListMyOrdersQuery(c.arg(PageArg), c.arg(SizeArg))))
        }
    ),
    Field(
      "order",
      OrderType,
      arguments = List(IdArg),
      resolve = c => RoleGuard.require(c.ctx.actor, Client)(AppErrorException.orFail(getMyOrder.execute(c.ctx.actor, c.arg(IdArg))))
    ),
    Field(
      "orders",
      OrderPageType,
      arguments = List(PageArg, SizeArg),
      resolve = c =>
        RoleGuard.require(c.ctx.actor, Manager) {
          AppErrorException.orFail(listOrders.execute(ListOrdersQuery(c.arg(PageArg), c.arg(SizeArg))))
        }
    )
  )

  val mutationFields: List[Field[GraphQLContext, Unit]] = List(
    Field(
      "checkout",
      OrderType,
      resolve = c => RoleGuard.require(c.ctx.actor, Client)(AppErrorException.orFail(checkout.execute(c.ctx.actor)))
    ),
    Field(
      "shipOrder",
      OrderType,
      arguments = List(IdArg, TrackingNumberArg, EstimatedDeliveryArg),
      resolve = c =>
        changeStatus(c.ctx.actor, c.arg(IdArg), Shipped, Some(c.arg(TrackingNumberArg)), Some(c.arg(EstimatedDeliveryArg)))
    ),
    Field("deliverOrder", OrderType, arguments = List(IdArg), resolve = c => changeStatus(c.ctx.actor, c.arg(IdArg), Delivered, None, None)),
    Field("cancelOrder", OrderType, arguments = List(IdArg), resolve = c => changeStatus(c.ctx.actor, c.arg(IdArg), Cancelled, None, None))
  )

  private def changeStatus(
    actor: Actor,
    id: String,
    to: OrderStatus,
    trackingNumber: Option[String],
    estimatedDelivery: Option[String]
  ): Future[Order] =
    RoleGuard.require(actor, Manager)(AppErrorException.orFail(changeOrderStatus.execute(actor, id, to, trackingNumber, estimatedDelivery)))
}
