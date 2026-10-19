package com.mendev.apistore.ordering.application

import java.util.UUID

trait OrderIdGenerator {
  def next(): UUID
}
