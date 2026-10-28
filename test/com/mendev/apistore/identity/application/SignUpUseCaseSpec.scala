package com.mendev.apistore.identity.application

import com.mendev.apistore.identity.domain.User
import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.AppError
import com.mendev.apistore.shared.security.Client
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.lang.reflect.Proxy
import java.sql.Connection
import java.time.{Clock, Instant, ZoneOffset}
import scala.concurrent.duration.*
import scala.concurrent.{Await, ExecutionContext, Future}

class SignUpUseCaseSpec extends AnyWordSpec with Matchers {

  private implicit val ec: ExecutionContext = ExecutionContext.global

  private val fixedId = "0192f1c8-7b3e-7a10-8c44-5d2f6e9a1b07"
  private val now     = Instant.parse("2026-01-01T00:00:00Z")
  private val clock   = Clock.fixed(now, ZoneOffset.UTC)

  private val conn: Connection =
    Proxy
      .newProxyInstance(getClass.getClassLoader, Array(classOf[Connection]), (_, _, _) => throw new UnsupportedOperationException)
      .asInstanceOf[Connection]

  private val idGenerator: IdGenerator = () => fixedId

  private val hasher: PasswordHasher = new PasswordHasher {
    def hash(userPwd: String): String                          = "hashed"
    def verify(plainPwd: String, storedHash: String): Boolean = ???
  }

  private val runner: TxRunner = new TxRunner {
    def run[E, A](work: Connection => Either[E, A]): Future[Either[E, A]] = Future.successful(work(conn))
  }

  private class FakeUserRepository extends UserRepository {
    var saved: List[User] = Nil

    def save(user: User, conn: Connection): Either[AppError, Unit] = {
      saved = saved :+ user
      Right(())
    }
    def findByEmail(email: String, conn: Connection): Either[AppError, Option[User]]                          = ???
    def findByPublicId(publicId: String, conn: Connection): Either[AppError, Option[User]]                    = ???
    def findByPublicIds(publicIds: List[String], conn: Connection): Either[AppError, List[User]]              = ???
    def incrementTokenVersion(publicId: String, now: Instant, conn: Connection): Either[AppError, Unit]       = ???
    def updatePasswordHash(publicId: String, newHash: String, now: Instant, conn: Connection): Either[AppError, Unit] = ???
  }

  private def signUp(repo: FakeUserRepository, command: SignUpCommand): Either[AppError, SignUpResponse] =
    Await.result(new SignUpUseCase(idGenerator, repo, clock, hasher, runner).execute(command), 10.seconds)

  "The sign-up use case" should {
    "save a normalized client user and return its public id" in {
      val repo = new FakeUserRepository
      signUp(repo, SignUpCommand("  Ana@Example.COM ", " Ana ", " Pérez ", "password1")) shouldBe Right(SignUpResponse(fixedId))
      repo.saved shouldBe List(User(fixedId, "ana@example.com", "Ana", "Pérez", "hashed", Client, now, now, 0))
    }

    "report every invalid field together and save nothing" in {
      val repo = new FakeUserRepository
      signUp(repo, SignUpCommand("bad", " ", "Pérez", "short")) match {
        case Left(AppError.Validation(_, details)) =>
          details.map(_.field) should contain theSameElementsAs List("email", "password", "firstName")
        case other => fail(s"Expected a validation error, got $other")
      }
      repo.saved shouldBe empty
    }
  }
}
