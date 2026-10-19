package com.mendev.apistore.graphql

import com.mendev.apistore.ordering.application.CheckoutUseCase
import com.mendev.apistore.shared.SharedComponents
import com.mendev.apistore.shared.actions.AuthenticatedAction
import com.mendev.apistore.shopping.application.{AddToCartUseCase, GetCartUseCase, RemoveFromCartUseCase, UpdateCartItemQuantityUseCase}
import play.api.ContextBasedBuiltInComponents
import sangria.schema.Schema

trait GraphQLComponents {
  this: ContextBasedBuiltInComponents with SharedComponents =>

  def authenticatedAction: AuthenticatedAction
  def addToCartUseCase: AddToCartUseCase
  def updateCartItemQuantityUseCase: UpdateCartItemQuantityUseCase
  def removeFromCartUseCase: RemoveFromCartUseCase
  def getCartUseCase: GetCartUseCase
  def checkoutUseCase: CheckoutUseCase

  lazy val shoppingSchema: ShoppingSchema =
    new ShoppingSchema(addToCartUseCase, updateCartItemQuantityUseCase, removeFromCartUseCase, getCartUseCase)(executionContext)

  lazy val orderingSchema: OrderingSchema = new OrderingSchema(checkoutUseCase)(executionContext)

  lazy val graphQLSchema: Schema[GraphQLContext, Unit] = new GraphQLSchema(shoppingSchema, orderingSchema).schema

  lazy val graphQLExecutor: GraphQLExecutor =
    new GraphQLExecutor(graphQLSchema, appConfig.graphql)(executionContext)

  lazy val graphQLController: GraphQLController =
    new GraphQLController(controllerComponents, graphQLExecutor, authenticatedAction)(executionContext)
}
