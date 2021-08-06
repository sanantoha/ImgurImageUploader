package com.leadiq.service

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors

import cats.effect.{ContextShift, IO, Sync}
import cats.syntax.applicative._
import cats.syntax.option._
import com.leadiq.UnitSpec
import com.leadiq.algebra.ImgurImageUploader
import com.leadiq.config.{Config, Connection, Job, Server}
import com.leadiq.domain._
import io.circe._
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.dsl.io._
import org.http4s.{EntityDecoder, EntityEncoder, Request, Status, Uri}
import ImgurImageUploaderImpl._
import cats.syntax.functor._

import scala.concurrent.ExecutionContext

class ImgurImageUploaderHttpSpec extends UnitSpec {

  val testEc = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())
  implicit val cs: ContextShift[IO] = IO.contextShift(testEc)

  implicit val decoder: EntityDecoder[IO, Json] = jsonOf[IO, Json]
  implicit val encoder: EntityEncoder[IO, Json] = jsonEncoderOf[IO, Json]

  val config = Config("clientId", "imgurApi", Connection(1, 1, 1), Server("host", 0, "v1"), Job("*/1 * * ? * *", 1))

  val expUrl1 = "https://i.imgur.com/gAGub9k.jpg"
  val expUrl2 = "https://i.imgur.com/skSpO.jpg"

  "ImgurImageUploaderHttp" should "return all uploaded links on GET query" in {
    val expJson = Json.obj(
      ("uploaded", Json.arr(
        Json.fromString(expUrl1),
        Json.fromString(expUrl2)
      ))
    )

    val req = Request[IO](uri = Uri(path = "/v1/images"))

    val (status, ioBody) = runRequest[IO, Json](req,
      none,
      UploadedLinksResponse(List(expUrl1, expUrl2)),
      UploadResponse("jobId")
    ).unsafeRunSync()

    (status, ioBody.unsafeRunSync()) shouldBe ((Status.Ok, expJson))
  }

  it should "return job status on GET query" in {
    val expJobId = "55355b7c-9b86-4a1a-b32e-6cdd6db07183"
    val expStartDateTime = LocalDateTime.now
    val expPendingUrl = "https://www.factslides.com/imgs/white-cat.jpg"
    val expFailedUrl = "https://www.factslides.com/imgs/black-cat.jpg"

    val expJson = Json.obj(
      ("id", Json.fromString(expJobId)),
      ("created", Json.fromString(expStartDateTime.format(DateTimeFormatter.ISO_DATE_TIME))),
      ("finished", Json.Null),
      ("status", Json.fromString(InProgressJobStatus)),
      ("uploaded", Json.obj(
        ("pending", Json.arr(Json.fromString(expPendingUrl))),
        ("complete", Json.arr(
          Json.fromString(expUrl1),
          Json.fromString(expUrl2)
        )),
        ("failed", Json.arr(Json.fromString(expFailedUrl)))
      ))
    )

    val req = Request[IO](uri = Uri(path = s"/v1/images/upload/$expJobId"))

    val uploadJobStatusResponse = UploadJobStatusResponse(
      expJobId,
      expStartDateTime,
      none,
      InProgressJobStatus,
      Uploaded(
        List(expPendingUrl),
        List(expUrl1, expUrl2),
        List(expFailedUrl)
      )
    )

    val (status, ioBody) = runRequest[IO, Json](req,
      uploadJobStatusResponse.some,
      UploadedLinksResponse(Nil),
      UploadResponse("jobId")
    ).unsafeRunSync()


    (status, ioBody.unsafeRunSync()) shouldBe ((Status.Ok, expJson))
  }

  it should "publish new task and return jobId on POST query" in {
    val url1 = "https://farm3.staticflickr.com/2879/11234651086_681b3c2c00_b_d.jpg"
    val url2 = "https://farm4.staticflickr.com/3790/11244125445_3c2f32cd83_k_d.jpg"
    val queryJson = Json.obj(
      ("urls", Json.arr(
        Json.fromString(url1),
        Json.fromString(url2)
      ))
    )

    val expJobId = "55355b7c-9b86-4a1a-b32e-6cdd6db07183"
    val expJson = Json.obj(
      ("jobId", Json.fromString(expJobId))
    )

    val req = Request[IO](method = POST, uri = Uri(path = "/v1/images/upload")).withEntity(queryJson)

    val (status, ioBody) = runRequest[IO, Json](req,
      none,
      UploadedLinksResponse(Nil),
      UploadResponse(expJobId)
    ).unsafeRunSync()


    (status, ioBody.unsafeRunSync()) shouldBe ((Status.Ok, expJson))
  }

  def runRequest[F[_] : Sync, A](req: Request[F],
                                 js: Option[UploadJobStatusResponse],
                                 ulr: UploadedLinksResponse,
                                 ur: UploadResponse)(implicit ev: EntityDecoder[F, A]): F[(Status, F[A])] = {

    val imgurImageUploader = new ImgurImageUploader[F] {

      override def jobStatus(jobId: String): F[Option[UploadJobStatusResponse]] = js.pure[F]

      override def allUploadedLinks(): F[UploadedLinksResponse] = ulr.pure[F]

      override def uploadImages(uploadRequest: UploadRequest): F[UploadResponse] = ur.pure[F]
    }


    val imgurImageUploaderHttp = new ImgurImageUploaderHttp[F](config, imgurImageUploader)

    val res = imgurImageUploaderHttp.service.orNotFound.run(req)

    res.map(r =>
      (r.status, r.as[A])
    )
  }
}
