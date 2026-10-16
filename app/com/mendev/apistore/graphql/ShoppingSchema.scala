package com.mendev.apistore.graphql

import com.mendev.apistore.shared.error.AppError
import com.mendev.apistore.shared.security.{Actor, Client}
import com.mendev.apistore.shopping.application.{AddToCartCommand, AddToCartUseCase, CartLine, CartView, GetCartUseCase, RemoveFromCartUseCase, UpdateCartItemQuantityCommand, UpdateCartItemQuantityUseCase}
import sangria.schema.{Argument, BigDecimalType, BooleanType, Field, IntType, ListType, LongType, ObjectType, StringType, fields}

import scala.concurrent.{ExecutionContext, Future}

class ShoppingSchema(
  addToCart: AddToCartUseCase,
  updateQuantity: UpdateCartItemQuantityUseCase,
  removeFromCart: RemoveFromCartUseCase,
  getCart: GetCartUseCase
)(implicit ec: ExecutionContext) {

  private val CartLineType: ObjectType[GraphQLContext, CartLine] = ObjectType(
    "CartLine",
    fields[GraphQLContext, CartLine](
      Field("productId", LongType, resolve = _.value.productId),
      Field("name", StringType, resolve = _.value.name),
      Field("quantity", IntType, resolve = _.value.quantity),
      Field("price", BigDecimalType, resolve = _.value.price),
      Field("discountPercent", BigDecimalType, resolve = _.value.discountPercent),
      Field("discountedPrice", BigDecimalType, resolve = _.value.discountedPrice),
      Field("lineTotal", BigDecimalType, resolve = _.value.lineTotal),
      Field("available", BooleanType, resolve = _.value.available)
    )
  )

  private val CartType: ObjectType[GraphQLContext, CartView] = ObjectType(
    "Cart",
    fields[GraphQLContext, CartView](
      Field("lines", ListType(CartLineType), resolve = _.value.lines),
      Field("total", BigDecimalType, resolve = _.value.total)
    )
  )

  private val ProductIdArg = Argument("productId", LongType)
  private val QuantityArg  = Argument("quantity", IntType)

  val queryFields: List[Field[GraphQLContext, Unit]] = List(
    Field("myCart", CartType, resolve = c => RoleGuard.require(c.ctx.actor, Client)(cart(c.ctx.actor)))
  )

  val mutationFields: List[Field[GraphQLContext, Unit]] = List(
    Field(
      "addToCart",
      CartType,
      arguments = List(ProductIdArg, QuantityArg),
      resolve = c =>
        RoleGuard.require(c.ctx.actor, Client) {
          thenCart(c.ctx.actor)(addToCart.execute(c.ctx.actor, AddToCartCommand(c.arg(ProductIdArg), c.arg(QuantityArg))))
        }
    ),
    Field(
      "updateCartItemQuantity",
      CartType,
      arguments = List(ProductIdArg, QuantityArg),
      resolve = c =>
        RoleGuard.require(c.ctx.actor, Client) {
          thenCart(c.ctx.actor)(updateQuantity.execute(c.ctx.actor, UpdateCartItemQuantityCommand(c.arg(ProductIdArg), c.arg(QuantityArg))))
        }
    ),
    Field(
      "removeFromCart",
      CartType,
      arguments = List(ProductIdArg),
      resolve = c =>
        RoleGuard.require(c.ctx.actor, Client) {
          thenCart(c.ctx.actor)(removeFromCart.execute(c.ctx.actor, c.arg(ProductIdArg)))
        }
    )
  )

  private def cart(actor: Actor): Future[CartView] =
    AppErrorException.orFail(getCart.execute(actor))

  private def thenCart(actor: Actor)(change: Future[Either[AppError, Unit]]): Future[CartView] =
    AppErrorException.orFail(change).flatMap(_ => cart(actor))
}
