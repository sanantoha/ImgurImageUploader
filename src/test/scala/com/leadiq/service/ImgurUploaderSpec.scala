package com.leadiq.service

import java.util.concurrent.Executors

import cats.Applicative
import cats.effect.concurrent.Ref
import cats.effect.{ConcurrentEffect, ContextShift, IO, Sync}
import com.leadiq.UnitSpec
import com.leadiq.domain.{ImgurUploadData, ImgurUploadResponse}
import org.http4s.{EntityEncoder, HttpApp, Method, Request, Response}
import org.http4s.client.Client
import org.http4s.dsl.io.Ok
import cats.syntax.functor._
import io.chrisdavenport.log4cats.noop.NoOpLogger
import cats.syntax.option._
import com.leadiq.config.{Config, Connection, Job, Server}
import org.http4s.circe._
import io.circe.generic.auto._
import cats.syntax.flatMap._
import com.leadiq.domain.ImgUpError.WrongUrl
import cats.syntax.either._
import com.leadiq.Utils.ErrorUrl

import scala.concurrent.ExecutionContext

class ImgurUploaderSpec extends UnitSpec {

  val testEc = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())
  implicit val cs: ContextShift[IO] = IO.contextShift(testEc)

  implicit def encoder[F[_]: Applicative]: EntityEncoder[F, ImgurUploadResponse] = jsonEncoderOf[F, ImgurUploadResponse]

  val expResonse = ImgurUploadResponse(
    ImgurUploadData("aWWkdF5", none, none, 1544790616, "image/jpeg", false, 225, 225, 9113, 0, 0, none, false,
      none, none, none, 0, false, false, false, Nil, 0, "", false, "SKvOK28OTUdvdRS", "", "https://i.imgur.com/aWWkdF5.jpg"),
    true, 200
  )

  "ImgurUploader" should "upload image" in {
    val expUrl = "http://imgur.com"
    val clientId = "clientId"

    run[IO](expUrl, clientId, expResonse).unsafeRunSync() shouldBe ((expUrl, Method.POST, s"Client-ID $clientId".some, expResonse))
  }

  it should "return exception on wrong url " in {
    val expUrl = "wrong url"

    run[IO](expUrl, "clientId", expResonse).attempt.unsafeRunSync() shouldBe WrongUrl(ErrorUrl).asLeft[(String, Method, Option[String], ImgurUploadResponse)]
  }

  def run[F[_]: ConcurrentEffect](url: String,
                                  clientId: String,
                                  expResponse: ImgurUploadResponse): F[(String, Method, Option[String], ImgurUploadResponse)] = {
    for {
      reqRef <- Ref.of(Request[F]())

      client = mkClientForUploadImg(reqRef, expResponse)
      logger = NoOpLogger.impl[F]
      config = Config(clientId, url, Connection(1, 1, 1), Server("host", 0, "v1"), Job("*/1 * * ? * *", 1))

      uploader = new ImgurUploaderImpl[F](config, client, logger)
      imgUploadResponse <- uploader.upload(Array[Byte](1, 2, 3))

      req <- reqRef.get
      headerValue = req.headers.headOption.map(_.value)
    } yield (req.uri.toString, req.method, headerValue, imgUploadResponse)
  }

  def mkClientForUploadImg[F[_] : Sync](ref: Ref[F, Request[F]], expResponse: ImgurUploadResponse): Client[F] = {
    val httpApp = HttpApp[F] {
      r => ref.set(r) as Response[F](Ok).withEntity(expResponse)
    }

    Client.fromHttpApp[F](httpApp)
  }
}
