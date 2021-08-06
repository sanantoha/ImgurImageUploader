package com.leadiq.service

import java.time.LocalDateTime
import java.util.concurrent.Executors

import cats.effect._
import cats.effect.concurrent.Ref
import cats.instances.string._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.option.{none, _}
import cats.temp.par.Par
import cats.{Applicative, Monad}
import com.leadiq.UnitSpec
import com.leadiq.Utils._
import com.leadiq.config.{Config, Connection, Job, Server}
import com.leadiq.domain._
import com.leadiq.service.ImgurImageUploaderImpl._
import io.chrisdavenport.log4cats.noop.NoOpLogger

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class JobTaskSchedulerSpec extends UnitSpec {

  val testEc = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())
  implicit val cs: ContextShift[IO] = IO.contextShift(testEc)
  implicit val timer: Timer[IO] = IO.timer(testEc)

  "JobTaskScheduler" should "" in {
    val expJobId = "55355b7c-9b86-4a1a-b32e-6cdd6db07183"
    val expStartDateTime = LocalDateTime.now

    val expArr = Array[Byte](1, 2, 3)

    val expUrl1 = "https://farm3.staticflickr.com/2879/11234651086_681b3c2c00_b_d.jpg"
    val expComplete1 = "https://i.imgur.com/gAGub9k.jpg"

    val expImgurUploadResponse = ImgurUploadResponse(
      ImgurUploadData("aWWkdF5", none, none, 1544790616, "image/jpeg", false, 225, 225, 9113, 0, 0, none, false,
        none, none, none, 0, false, false, false, Nil, 0, "", false, "SKvOK28OTUdvdRS", "", expComplete1),
      true, 200
    )

    val initJobStatusResponse = UploadJobStatusResponse(expJobId, expStartDateTime, none,
      PendingJobStatus, Uploaded(List(expUrl1), List(), List()))

    val expJobStatusResponse = UploadJobStatusResponse(expJobId, expStartDateTime, expStartDateTime.some,
      CompleteJobStatus, Uploaded(List(), List(expComplete1), List()))

    run[IO](expStartDateTime,
      expArr,
      expImgurUploadResponse,
      List(expJobStatusResponse, initJobStatusResponse)).unsafeRunSync() shouldBe ((expUrl1, expArr, expJobId, expComplete1, expJobStatusResponse))
  }

  def run[F[_]: Concurrent: Par: Timer](expStartDateTime: LocalDateTime,
                                        arr: Array[Byte],
                                        imgurUploaderResponse: ImgurUploadResponse,
                                        jobStoreState: List[UploadJobStatusResponse]
                                       ): F[(String, Array[Byte], String, String, UploadJobStatusResponse)] = {
    for {
      urlRef <- Ref.of("")
      arrRef <- Ref.of(Array[Byte]())
      keyJobStoreRef <- Ref.of("")
      keyLinkStoreRef <- Ref.of("")
      valLinkStoreRef <- Ref.of("")
      valJobStoreRef <- Ref.of(dummyUploadJobStatusResponse)

      logger = NoOpLogger.impl[F]
      loader = mkLoader[F](urlRef, arr)
      uploader = mkUploader[F](arrRef, imgurUploaderResponse)
      jobStore = mkKVStore[F, UploadJobStatusResponse](keyJobStoreRef, valJobStoreRef, jobStoreState)
      linkStore = mkKVStore[F, String](keyLinkStoreRef, valLinkStoreRef, Nil)
      config = Config("clientId", "imgurApi", Connection(1, 1, 1), Server("host", 0, "V1"), Job("cron", 1))
      dateTime = mkDateTime[F](expStartDateTime)

      jobTaskScheduler = new JobTaskSchedulerImpl[F](config, loader, uploader, jobStore, linkStore, dateTime, logger)

      _ <- jobTaskScheduler.execute()
      _ <- waitUntil[F](urlRef)
      url <- urlRef.get
      arrRes <- arrRef.get
      keyJobStore <- keyJobStoreRef.get
      valJobStore <- valJobStoreRef.get
      valLinkStore <- valLinkStoreRef.get
    } yield (url, arrRes, keyJobStore, valLinkStore, valJobStore)
  }

  def waitUntil[F[_]: Monad: Timer](ref: Ref[F, String]): F[Unit] = for {
    r <- ref.get
    _ <- if (r === "") Timer[F].sleep(100.milliseconds) *> waitUntil(ref)
    else Applicative[F].unit
  } yield ()
}
