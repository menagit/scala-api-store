package com.mendev.apistore.graphql

import com.mendev.apistore.shared.security.{Actor, Client, Manager, Role}
import sangria.schema.{Field, ObjectType, Schema, StringType, fields}

object GraphQLSchema {

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
    )
  )

  val schema: Schema[GraphQLContext, Unit] = Schema(QueryType)

  private def roleText(role: Role): String = role match {
    case Manager => "MANAGER"
    case Client  => "CLIENT"
  }
}
