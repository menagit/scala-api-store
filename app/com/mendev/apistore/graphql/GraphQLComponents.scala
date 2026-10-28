package com.mendev.apistore.graphql

import com.mendev.apistore.catalog.application.{ChangeProductStatusUseCase, DeleteProductUseCase}
import com.mendev.apistore.ordering.application.{ChangeOrderStatusUseCase, CheckoutUseCase, GetMyOrderUseCase, GetOrderClientsUseCase, ListMyOrdersUseCase, ListOrdersUseCase}
import com.mendev.apistore.shared.SharedComponents
import com.mendev.apistore.shared.actions.AuthenticatedAction
import com.mendev.apistore.shopping.application.{AddToCartUseCase, GetCartUseCase, RemoveFromCartUseCase, UpdateCartItemQuantityUseCase}
import play.api.ContextBasedBuiltInComponents
import sangria.execution.deferred.DeferredResolver
import sangria.schema.Schema

trait GraphQLComponents {
  this: ContextBasedBuiltInComponents with SharedComponents =>

  def authenticatedAction: AuthenticatedAction
  def addToCartUseCase: AddToCartUseCase
  def updateCartItemQuantityUseCase: UpdateCartItemQuantityUseCase
  def removeFromCartUseCase: RemoveFromCartUseCase
  def getCartUseCase: GetCartUseCase
  def checkoutUseCase: CheckoutUseCase
  def listMyOrdersUseCase: ListMyOrdersUseCase
  def getMyOrderUseCase: GetMyOrderUseCase
  def listOrdersUseCase: ListOrdersUseCase
  def getOrderClientsUseCase: GetOrderClientsUseCase
  def changeOrderStatusUseCase: ChangeOrderStatusUseCase
  def changeProductStatusUseCase: ChangeProductStatusUseCase
  def deleteProductUseCase: DeleteProductUseCase

  lazy val shoppingSchema: ShoppingSchema =
    new ShoppingSchema(addToCartUseCase, updateCartItemQuantityUseCase, removeFromCartUseCase, getCartUseCase)(executionContext)

  lazy val orderingSchema: OrderingSchema =
    new OrderingSchema(checkoutUseCase, listMyOrdersUseCase, getMyOrderUseCase, listOrdersUseCase, getOrderClientsUseCase, changeOrderStatusUseCase)(executionContext)

  lazy val catalogSchema: CatalogSchema =
    new CatalogSchema(changeProductStatusUseCase, deleteProductUseCase)(executionContext)

  lazy val graphQLSchema: Schema[GraphQLContext, Unit] = new GraphQLSchema(shoppingSchema, orderingSchema, catalogSchema).schema

  lazy val graphQLDeferredResolver: DeferredResolver[GraphQLContext] = DeferredResolver.fetchers(orderingSchema.clientFetcher)

  lazy val graphQLExecutor: GraphQLExecutor =
    new GraphQLExecutor(graphQLSchema, graphQLDeferredResolver, appConfig.graphql)(executionContext)

  lazy val graphQLController: GraphQLController =
    new GraphQLController(controllerComponents, graphQLExecutor, authenticatedAction)(executionContext)
}
