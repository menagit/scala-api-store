package com.mendev.apistore.graphql

import com.mendev.apistore.shared.SharedComponents
import com.mendev.apistore.shared.actions.AuthenticatedAction
import play.api.ContextBasedBuiltInComponents
import sangria.schema.Schema

trait GraphQLComponents {
  this: ContextBasedBuiltInComponents with SharedComponents =>

  def authenticatedAction: AuthenticatedAction

  lazy val graphQLSchema: Schema[GraphQLContext, Unit] = GraphQLSchema.schema

  lazy val graphQLExecutor: GraphQLExecutor =
    new GraphQLExecutor(graphQLSchema, appConfig.graphql)(executionContext)

  lazy val graphQLController: GraphQLController =
    new GraphQLController(controllerComponents, graphQLExecutor, authenticatedAction)(executionContext)
}
