package com.mendev.apistore.catalog.application

import com.mendev.apistore.catalog.domain.{Category, Product, ProductStatus}
import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.{AppError, FieldError}

import scala.concurrent.Future

class ListManagerProductsUseCase(productRepo: ProductRepository, categoryRepo: CategoryRepository, runner: TxRunner) {

  def execute(query: ListManagerProductsQuery): Future[Either[AppError, ProductPage]] = {
    val page           = parseInt("page", query.page, 1).map(p => if (p < 1) 1 else p)
    val size           = parseInt("size", query.size, 20).map(s => if (s < 1) 20 else math.min(s, 100))
    val sort           = parseSort(query.sort)
    val order          = parseOrder(query.order)
    val categoryId     = parseCategoryId(query.categoryId)
    val status         = parseStatus(query.status)
    val includeDeleted = parseIncludeDeleted(query.includeDeleted)
    (page, size, sort, order, categoryId, status, includeDeleted) match {
      case (Right(p), Right(s), Right(so), Right(o), Right(c), Right(st), Right(d)) =>
        load(p, s, so, o, ManagerProductFilter(c, st, d))
      case _ =>
        val errors = List(page, size, sort, order, categoryId, status, includeDeleted).collect { case Left(e) => e }
        Future.successful(Left(AppError.Validation("Invalid request", errors)))
    }
  }

  private def load(page: Int, size: Int, sort: ProductSort, order: SortOrder, filter: ManagerProductFilter): Future[Either[AppError, ProductPage]] =
    runner.run { conn =>
      productRepo.countManager(filter, conn) match {
        case Left(e) => Left(e)
        case Right(total) =>
          productRepo.findManagerPage(filter, sort, order, size, (page - 1).toLong * size, conn) match {
            case Left(e) => Left(e)
            case Right(products) =>
              categoryRepo.findAll(conn).map(categories => toPage(products, categories, page, size, total))
          }
      }
    }

  private def toPage(products: List[Product], categories: List[Category], page: Int, size: Int, total: Long): ProductPage = {
    val byId = categories.map(c => c.id -> c).toMap
    val items = products.map { p =>
      ProductListItem(p, byId.getOrElse(p.categoryId, throw new IllegalStateException(s"Missing category ${p.categoryId} for product ${p.id}")))
    }
    ProductPage(items, page, size, total, (total + size - 1) / size)
  }

  private def parseInt(field: String, text: Option[String], default: Int): Either[FieldError, Int] = text match {
    case None    => Right(default)
    case Some(t) => t.toIntOption.toRight(FieldError(field, "must be a whole number"))
  }

  private def parseCategoryId(text: Option[String]): Either[FieldError, Option[Long]] = text match {
    case None    => Right(None)
    case Some(t) => t.toLongOption.map(Some(_)).toRight(FieldError("categoryId", "must be a whole number"))
  }

  private def parseSort(text: Option[String]): Either[FieldError, ProductSort] = text match {
    case None              => Right(ProductSort.CreatedAt)
    case Some("price")     => Right(ProductSort.Price)
    case Some("name")      => Right(ProductSort.Name)
    case Some("createdAt") => Right(ProductSort.CreatedAt)
    case Some(_)           => Left(FieldError("sort", "must be price, name or createdAt"))
  }

  private def parseOrder(text: Option[String]): Either[FieldError, SortOrder] = text match {
    case None         => Right(SortOrder.Desc)
    case Some("asc")  => Right(SortOrder.Asc)
    case Some("desc") => Right(SortOrder.Desc)
    case Some(_)      => Left(FieldError("order", "must be asc or desc"))
  }

  private def parseStatus(text: Option[String]): Either[FieldError, Option[ProductStatus]] = text match {
    case None             => Right(None)
    case Some("ACTIVE")   => Right(Some(ProductStatus.Active))
    case Some("DISABLED") => Right(Some(ProductStatus.Disabled))
    case Some(_)          => Left(FieldError("status", "must be ACTIVE or DISABLED"))
  }

  private def parseIncludeDeleted(text: Option[String]): Either[FieldError, Boolean] = text match {
    case None          => Right(false)
    case Some("true")  => Right(true)
    case Some("false") => Right(false)
    case Some(_)       => Left(FieldError("includeDeleted", "must be true or false"))
  }
}
