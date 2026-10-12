package com.mendev.apistore.shared.actions

import com.mendev.apistore.shared.security.Actor
import play.api.mvc.{Request, WrappedRequest}

class AuthenticatedRequest[A](val actor: Actor,request: Request[A]) extends WrappedRequest[A](request)
