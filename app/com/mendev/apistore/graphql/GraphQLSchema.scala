package com.mendev.apistore.graphql

import com.mendev.apistore.shared.security.{Actor, Client, Manager, Role}
import sangria.schema.{Field, ObjectType, Schema, StringType, fields}

class GraphQLSchema(shopping: ShoppingSchema) {

  private val MeType: ObjectType[GraphQLContext, Actor] = ObjectType(
    "Me",
    fields[GraphQLContext, Actor](
      Field("publicId", StringType, resolve = _.value.publicId),
      Field("role", StringType, resolve = c => roleText(c.value.role))
    )
  )

  private val QueryType: ObjectType[GraphQLContext, Unit] = ObjectType(
    "Query",
    fields[GraphQLContext, Unit](
      Field("me", MeType, resolve = _.ctx.actor)
    ) ++ shopping.queryFields
  )

  private val MutationType: ObjectType[GraphQLContext, Unit] = ObjectType("Mutation", shopping.mutationFields)

  val schema: Schema[GraphQLContext, Unit] = Schema(QueryType, Some(MutationType))

  private def roleText(role: Role): String = role match {
    case Manager => "MANAGER"
    case Client  => "CLIENT"
  }
}
