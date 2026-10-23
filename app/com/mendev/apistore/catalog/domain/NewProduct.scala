package com.mendev.apistore.catalog.domain

import com.mendev.apistore.shared.error.FieldError

import java.time.Instant

// NewProduct.create is the only way to build one
final case class NewProduct private (
        name: String,
        description: String,
        categoryId: Long,
        price: Price,
        discount: Discount,
        stock: Stock,
        status: ProductStatus,
        createdAt: Instant,
        updatedAt: Instant
                                    )

object NewProduct {

  // All field errors are reported together.
  def create(
              name: String,
              description: String,
              categoryId: Long,
              price: BigDecimal,
              discountPercent: BigDecimal,
              stock: Int,
              status: ProductStatus,
              now: Instant
            ): Either[List[FieldError], NewProduct] = {
    val nameResult        = ProductRules.name(name)
    val descriptionResult = ProductRules.description(description)
    val priceResult       = Price.from(price)
    val discountResult    = Discount.from(discountPercent)
    val stockResult       = Stock.from(stock)

    (nameResult, descriptionResult, priceResult, discountResult, stockResult) match {
      case (Right(n), Right(desc), Right(p), Right(d), Right(s)) =>
        Right(new NewProduct(n, desc, categoryId, p, d, s, status, now, now))
      case _ =>
        Left(List(nameResult, descriptionResult, priceResult, discountResult, stockResult).collect { case Left(e) => e })
    }
  }
}