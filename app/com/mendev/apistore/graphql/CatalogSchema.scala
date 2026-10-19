package com.mendev.apistore.graphql

import com.mendev.apistore.catalog.application.{ChangeProductStatusUseCase, DeleteProductUseCase}
import com.mendev.apistore.catalog.domain.Product
import com.mendev.apistore.catalog.domain.ProductStatus.{Active, Disabled}
import com.mendev.apistore.shared.error.AppError
import com.mendev.apistore.shared.security.{Actor, Manager}
import sangria.schema.{Argument, BigDecimalType, BooleanType, Field, IntType, LongType, ObjectType, StringType, fields}

import scala.concurrent.{ExecutionContext, Future}

class CatalogSchema(
  changeProductStatus: ChangeProductStatusUseCase,
  deleteProduct: DeleteProductUseCase
)(implicit ec: ExecutionContext) {

  private val ProductType: ObjectType[GraphQLContext, Product] = ObjectType(
    "Product",
    fields[GraphQLContext, Product](
      Field("id", LongType, resolve = _.value.id),
      Field("name", StringType, resolve = _.value.name),
      Field("price", BigDecimalType, resolve = _.value.price.value),
      Field("discountPercent", BigDecimalType, resolve = _.value.discount.percent),
      Field("discountedPrice", BigDecimalType, resolve = _.value.discountedPrice),
      Field("stock", IntType, resolve = _.value.stock.value),
      Field("status", StringType, resolve = _.value.status.toString.toUpperCase),
      Field("deleted", BooleanType, resolve = _.value.isDeleted)
    )
  )

  private val IdArg = Argument("id", LongType)

  val mutationFields: List[Field[GraphQLContext, Unit]] = List(
    Field(
      "disableProduct",
      ProductType,
      arguments = List(IdArg),
      resolve = c => asManager(c.ctx.actor)(changeProductStatus.execute(c.ctx.actor, c.arg(IdArg), Disabled))
    ),
    Field(
      "enableProduct",
      ProductType,
      arguments = List(IdArg),
      resolve = c => asManager(c.ctx.actor)(changeProductStatus.execute(c.ctx.actor, c.arg(IdArg), Active))
    ),
    Field(
      "deleteProduct",
      ProductType,
      arguments = List(IdArg),
      resolve = c => asManager(c.ctx.actor)(deleteProduct.execute(c.ctx.actor, c.arg(IdArg)))
    )
  )

  private def asManager[A](actor: Actor)(work: => Future[Either[AppError, A]]): Future[A] =
    RoleGuard.require(actor, Manager)(AppErrorException.orFail(work))
}
