package com.leadiq.service

import java.util.concurrent.Executors

import cats.effect.{ConcurrentEffect, ContextShift, IO, Sync}
import com.leadiq.UnitSpec
import com.leadiq.domain.ImgUpError.WrongUrl
import io.chrisdavenport.log4cats.noop.NoOpLogger
import cats.syntax.either._
import cats.syntax.applicative._
import org.http4s.{HttpApp, Response}
import org.http4s.client.Client
import org.http4s.dsl.io.Ok
import com.leadiq.Utils.ErrorUrl

import scala.concurrent.ExecutionContext

class ImgLoaderSpec extends UnitSpec {

  val testEc = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())
  implicit val cs: ContextShift[IO] = IO.contextShift(testEc)

  "ImgLoader" should "load images" in {
    val url = "http://localhost:8080/img.jpeg"
    val expRes = "image"

    run[IO](url, expRes).unsafeRunSync() shouldBe expRes.toCharArray.map(_.toByte)
  }

  it should "return exception on wrong url" in {
    run[IO]("wrong url", "image").attempt.unsafeRunSync() shouldBe WrongUrl(ErrorUrl).asLeft[Array[Byte]]
  }

  def mkClient[F[_]: Sync](response: String): Client[F] = {
    val httpApp = HttpApp[F](_ => Response[F](Ok).withEntity(response).pure[F])

    Client.fromHttpApp[F](httpApp)
  }

  def run[F[_]: ConcurrentEffect](url: String, response: String): F[Array[Byte]] = {
    val client = mkClient[F](response)
    val logger = NoOpLogger.impl[F]
    val loader = new ImgLoaderImpl[F](client, logger)
    loader.load(url)
  }
}