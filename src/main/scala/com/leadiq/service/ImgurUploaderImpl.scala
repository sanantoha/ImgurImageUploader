package com.leadiq.service

import cats.effect.ConcurrentEffect
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.leadiq.algebra.ImgurUploader
import com.leadiq.config.Config
import com.leadiq.domain.ImgUpError.{ImgurErrorResponse, WrongUrl}
import com.leadiq.domain.ImgurUploadResponse
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import io.circe.generic.auto._
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.{EntityDecoder, Header, Headers, Method, Request, Response, Uri}


class ImgurUploaderImpl[F[_]: ConcurrentEffect](config: Config, client: Client[F], logger: Logger[F]) extends ImgurUploader[F] {

  implicit val decoder: EntityDecoder[F, ImgurUploadResponse] = jsonOf[F, ImgurUploadResponse]

  override def upload(array: Array[Byte]): F[ImgurUploadResponse] = {
    for {
      uri <- Uri.fromString(config.imgurApi).leftMap(e => WrongUrl(e.getMessage()): Throwable).raiseOrPure

      req = Request[F](
        method = Method.POST,
        uri = uri,
        headers = Headers(
          Header("Authorization", s"Client-ID ${config.clientId}")
        ),
        body = Stream.emits(array)
      )

      _ <- logger.info(s"upload to uri: $uri")
      res <- client.expectOr[ImgurUploadResponse](req)(onError)
      _ <- logger.info(s"file uploaded with status: ${res.status}, id: ${res.data.id}, link: ${res.data.link}")

    } yield res
  }

  def onError(res: Response[F]): F[Throwable] = {
    val status = res.status
    res.body.map(_.toChar).compile.toList.map(_.mkString).map(s => ImgurErrorResponse(s"Error $status $s"))
  }
}