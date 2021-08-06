package com.leadiq.service

import java.time.LocalDateTime
import java.util.concurrent.Executors

import cats.effect._
import cats.effect.concurrent.Ref
import cats.instances.string._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.option._
import cats.temp.par.Par
import com.leadiq.UnitSpec
import com.leadiq.domain._
import com.leadiq.service.ImgurImageUploaderImpl._
import io.chrisdavenport.log4cats.noop.NoOpLogger
import com.leadiq.Utils._
import scala.concurrent.ExecutionContext


class ImgurImageUploaderSpec extends UnitSpec {

  val testEc = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())
  implicit val cs: ContextShift[IO] = IO.contextShift(testEc)
  implicit val timer: Timer[IO] = IO.timer(testEc)

  val expJobId = "55355b7c-9b86-4a1a-b32e-6cdd6db07183"
  val expStartDateTime = LocalDateTime.now
  val expPendingUrl = "https://www.factslides.com/imgs/white-cat.jpg"
  val expComplete1 = "https://i.imgur.com/gAGub9k.jpg"
  val expComplete2 = "https://i.imgur.com/skSpO.jpg"
  val expFailedUrl = "https://www.factslides.com/imgs/black-cat.jpg"

  "ImgurImageUploader" should "return job status" in {
    val expUploadJobStatusResponse = UploadJobStatusResponse(expJobId, expStartDateTime, none, InProgressJobStatus, Uploaded(
      List(expPendingUrl),
      List(expComplete1, expComplete2),
      List(expFailedUrl)
    ))

    runJobStatus[IO](expJobId, expUploadJobStatusResponse).unsafeRunSync() shouldBe ((expJobId, expUploadJobStatusResponse.some))
  }

  it should "return all uploaded images" in {
    runAllUploadedLinks[IO](List(expComplete1, expComplete2)).unsafeRunSync() shouldBe UploadedLinksResponse(List(expComplete1, expComplete2))
  }

  it should "start task and return jobId" in {
    val expUrl1 = "https://farm3.staticflickr.com/2879/11234651086_681b3c2c00_b_d.jpg"
    val expUrl2 = "https://farm4.staticflickr.com/3790/11244125445_3c2f32cd83_k_d.jpg"
    val expUploadRequest = UploadRequest(List(expUrl1, expUrl1, expUrl1, expUrl2))

    val expUploadJobStatusResponse = UploadJobStatusResponse(expJobId, expStartDateTime, none,
      PendingJobStatus, Uploaded(List(expUrl1, expUrl2), List(), List()))

    runUploadImages[IO](expUploadRequest, expJobId)
      .unsafeRunSync() shouldBe ((expJobId, expUploadJobStatusResponse, UploadResponse(expJobId)))
  }

  def runJobStatus[F[_]: Concurrent: Par: Clock](jobId: String, jobStoreState: UploadJobStatusResponse): F[(String, Option[UploadJobStatusResponse])] = {
    for {
      ref <- Ref.of("")
      valJobStoreRef <- Ref.of(dummyUploadJobStatusResponse)
      keyJobStoreRef <- Ref.of("")

      logger = NoOpLogger.impl[F]
      jobStore = mkKVStore[F, UploadJobStatusResponse](keyJobStoreRef, valJobStoreRef, List(jobStoreState))
      linkStore = mkKVStore[F, String](ref, ref, Nil)
      dateTime = mkDateTime[F](expStartDateTime)

      imgurImageUploader = new ImgurImageUploaderImpl[F](jobStore, linkStore, dateTime, logger)
      res <- imgurImageUploader.jobStatus(jobId)
      invokedJobId <- keyJobStoreRef.get
    } yield (invokedJobId, res)
  }

  def runAllUploadedLinks[F[_]: Concurrent: Par: Clock](linkStoreState: List[String]): F[UploadedLinksResponse] = {
    for {
      ref <- Ref.of("")
      valJobStoreRef <- Ref.of(dummyUploadJobStatusResponse)

      logger = NoOpLogger.impl[F]
      jobStore = mkKVStore[F, UploadJobStatusResponse](ref, valJobStoreRef, Nil)
      linkStore = mkKVStore[F, String](ref, ref, linkStoreState)
      dateTime = mkDateTime[F](expStartDateTime)


      imgurImageUploader = new ImgurImageUploaderImpl[F](jobStore, linkStore, dateTime, logger)
      res <- imgurImageUploader.allUploadedLinks()
    } yield res
  }

  def runUploadImages[F[_]: Concurrent: Par: Clock](uploadRequest: UploadRequest,
                                             uuid: String): F[(String, UploadJobStatusResponse, UploadResponse)] = {
    for {
      ref <- Ref.of("")
      keyJobStoreRef <- Ref.of("")
      valJobStoreRef <- Ref.of(dummyUploadJobStatusResponse)

      logger = NoOpLogger.impl[F]
      jobStore = mkKVStore[F, UploadJobStatusResponse](keyJobStoreRef, valJobStoreRef, Nil)
      linkStore = mkKVStore[F, String](ref, ref, Nil)
      dateTime = mkDateTime[F](expStartDateTime)

      imgurImageUploader = new ImgurImageUploaderImpl[F](jobStore, linkStore, dateTime, logger) {
        override def createUuid: String = uuid
      }
      response <- imgurImageUploader.uploadImages(uploadRequest)
      keyJobStore <- keyJobStoreRef.get
      valJobStore <- valJobStoreRef.get
    } yield (keyJobStore, valJobStore, response)
  }
}