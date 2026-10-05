package com.mendev.apistore.identity.application

trait IdGenerator {
  def generatePublicId(): String
}