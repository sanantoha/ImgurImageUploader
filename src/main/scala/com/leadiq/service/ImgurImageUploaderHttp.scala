package com.leadiq.service

import cats.effect.Sync
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes}
import org.http4s.dsl.Http4sDsl
import cats.syntax.flatMap._
import com.leadiq.algebra.ImgurImageUploader
import com.leadiq.config.Config
import com.leadiq.domain.{UploadJobStatusResponse, UploadRequest, UploadResponse, UploadedLinksResponse}
import org.http4s.circe._
import io.circe.generic.auto._

class ImgurImageUploaderHttp[F[_]: Sync](config: Config, imgurImageUploader: ImgurImageUploader[F]) extends Http4sDsl[F] {

  implicit def uploadedLinksResponseEncoder: EntityEncoder[F, UploadedLinksResponse] = jsonEncoderOf[F, UploadedLinksResponse]
  implicit def uploadJobStatusResponseEncoder: EntityEncoder[F, UploadJobStatusResponse] = jsonEncoderOf[F, UploadJobStatusResponse]
  implicit def uploadResponseEncoder: EntityEncoder[F, UploadResponse] = jsonEncoderOf[F, UploadResponse]
  implicit val decoder: EntityDecoder[F, UploadRequest] = jsonOf[F, UploadRequest]

  val service: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / config.server.version / "images" =>
      imgurImageUploader.allUploadedLinks().flatMap(Ok(_))

    case GET -> Root / config.server.version / "images" / "upload" / jobId =>
      imgurImageUploader.jobStatus(jobId).flatMap(_.fold(NotFound())(Ok(_)))

    case req @ POST -> Root / config.server.version / "images" / "upload" =>
      req.as[UploadRequest].flatMap(imgurImageUploader.uploadImages).flatMap(Ok(_))
  }

}
