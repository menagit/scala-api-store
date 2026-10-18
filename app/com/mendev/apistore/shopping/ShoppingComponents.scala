package com.mendev.apistore.shopping

import com.mendev.apistore.catalog.application.GetProductSummariesUseCase
import com.mendev.apistore.shared.SharedComponents
import com.mendev.apistore.shopping.application.{AddToCartUseCase, CartRepository, ClearCartUseCase, GetCartUseCase, LockCartItemsUseCase, ProductCatalog, RemoveFromCartUseCase, UpdateCartItemQuantityUseCase}
import com.mendev.apistore.shopping.infrastructure.catalog.InProcessProductCatalog
import com.mendev.apistore.shopping.infrastructure.persistence.RelateCartRepository
import play.api.ContextBasedBuiltInComponents

trait ShoppingComponents {
  this: ContextBasedBuiltInComponents with SharedComponents =>

  def getProductSummariesUseCase: GetProductSummariesUseCase

  lazy val cartRepository: CartRepository = new RelateCartRepository

  lazy val productCatalog: ProductCatalog = new InProcessProductCatalog(getProductSummariesUseCase)(executionContext)

  lazy val getCartUseCase: GetCartUseCase =
    new GetCartUseCase(productCatalog, cartRepository, txRunner)(executionContext)

  lazy val addToCartUseCase: AddToCartUseCase =
    new AddToCartUseCase(productCatalog, cartRepository, txRunner, clock)(executionContext)

  lazy val removeFromCartUseCase: RemoveFromCartUseCase =
    new RemoveFromCartUseCase(cartRepository, txRunner, clock)

  lazy val updateCartItemQuantityUseCase: UpdateCartItemQuantityUseCase =
    new UpdateCartItemQuantityUseCase(productCatalog, removeFromCartUseCase, cartRepository, txRunner, clock)(executionContext)

  lazy val lockCartItemsUseCase: LockCartItemsUseCase = new LockCartItemsUseCase(cartRepository)

  lazy val clearCartUseCase: ClearCartUseCase = new ClearCartUseCase(cartRepository)
}
