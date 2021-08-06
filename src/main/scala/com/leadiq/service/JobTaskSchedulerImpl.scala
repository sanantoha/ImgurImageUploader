package com.leadiq.service

import java.time.LocalDateTime

import cats.effect.syntax.concurrent._
import cats.effect.{Concurrent, Timer}
import cats.instances.list._
import cats.instances.string._
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.apply._
import cats.syntax.either._
import cats.syntax.eq._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.option._
import cats.syntax.parallel._
import cats.syntax.show._
import cats.temp.par._
import com.leadiq.algebra._
import com.leadiq.config.Config
import com.leadiq.domain.ImgUpError.WrongCroneExpression
import com.leadiq.domain.{ImgurUploadResponse, UploadJobStatusResponse}
import cron4s.Cron
import cron4s.expr.CronExpr
import eu.timepit.fs2cron.awakeEveryCron
import fs2.Stream
import io.chrisdavenport.log4cats.Logger


class JobTaskSchedulerImpl[F[_]: Concurrent: Timer: Par](config: Config,
                                                         loader: ImgLoader[F],
                                                         uploader: ImgurUploader[F],
                                                         jobStore: KVStore[F, UploadJobStatusResponse],
                                                         linkStore: KVStore[F, String],
                                                         dateTime: DateTime[F],
                                                         logger: Logger[F]) extends JobTaskScheduler[F] {

  import ImgurImageUploaderImpl._

  def parseCron(cron: String): F[CronExpr] = {
    Cron.parse(cron).leftMap(e => WrongCroneExpression(e.getMessage): Throwable).raiseOrPure[F]
  }

  override def run(): F[Unit] = {
    val stream = for {
      cronExpr <- Stream.eval(parseCron(config.job.cron))
      _ <- awakeEveryCron[F](cronExpr)
      _ <- Stream.eval(execute())
    } yield ()

    stream.compile.drain
  }

  def execute(): F[Unit] = {
    val stream = for {
      _ <- Stream.eval(logger.info("start checking new tasks"))
      jobs <- Stream.eval(jobStore.values())
      job <- Stream.emits(jobs).covary[F].filter(_.status === PendingJobStatus).take(config.job.maxParallelTasks.toLong)
      _ <- Stream.eval(logger.info(s"takes job: ${job.id} for execution"))
      _ <- Stream.eval(uploadImagesTask(job.uploaded.pending, job.id).start)
    } yield ()

    stream.compile.drain
  }


  private def uploadImagesTask(urls: List[String], uuid: String): F[Unit] = {
    for {
      _ <- jobStore.update(uuid, uploadJobStatusResponse => uploadJobStatusResponse.copy(status = InProgressJobStatus))

      startedJob <- jobStore.get(uuid)
      _ <- logger.info(s"start new job $uuid ${show(startedJob)}")

      _ <- urls.parTraverse(uploadOneImageTask(uuid))

      localDateTime <- dateTime.now
      _ <- addFinalResultToResponse(uuid, localDateTime)

      finishedJob <- jobStore.get(uuid)
      _ <- logger.info(s"job $uuid finished ${show(finishedJob)}")
    } yield ()
  }

  private def show(job: Option[UploadJobStatusResponse]): String = job.fold("")(_.show)

  private def uploadOneImageTask(uuid: String)(url: String): F[Unit] = {
    for {
      eiImgArr <- loader.load(url).attempt

      eiImgurUploadRes <- eiImgArr match {
        case Right(arr) => uploader.upload(arr).attempt
        case Left(e) => logger.warn(s"Could not read url: $url, error ${e.getMessage}") *>
          jobStore.update(uuid, addErrorToResponse(url)) *>
          e.asLeft[ImgurUploadResponse].pure[F]
      }

      _ <- eiImgurUploadRes match {
        case Right(imgurUploadRes) => jobStore.update(uuid, addResultToResponse(url, imgurUploadRes)) *>
          linkStore.put(imgurUploadRes.data.id, imgurUploadRes.data.link)
        case Left(e) => logger.warn(s"Could not upload image url: $url, ${e.getMessage}") *>
          jobStore.update(uuid, addErrorToResponse(url))
      }
    } yield ()
  }

  private def addErrorToResponse(url: String)
                                (uploadJobStatusResponse: UploadJobStatusResponse): UploadJobStatusResponse = {

    val newPending = uploadJobStatusResponse.uploaded.pending.filterNot(_ == url)
    val newFailed =
      if (uploadJobStatusResponse.uploaded.failed.contains(url))
        uploadJobStatusResponse.uploaded.failed
      else url :: uploadJobStatusResponse.uploaded.failed

    val newUploaded = uploadJobStatusResponse.uploaded.copy(pending = newPending, failed = newFailed)

    uploadJobStatusResponse.copy(uploaded = newUploaded)
  }

  private def addResultToResponse(url: String, imgurUploadRes: ImgurUploadResponse)
                                 (uploadJobStatusResponse: UploadJobStatusResponse): UploadJobStatusResponse = {
    val newPending = uploadJobStatusResponse.uploaded.pending.filterNot(_ == url)

    val newCompleted = imgurUploadRes.data.link :: uploadJobStatusResponse.uploaded.complete

    val newUploaded = uploadJobStatusResponse.uploaded.copy(pending = newPending, complete = newCompleted)
    uploadJobStatusResponse.copy(uploaded = newUploaded)
  }

  def addFinalResultToResponse(uuid: String, time: LocalDateTime): F[Unit] = {
    jobStore.update(uuid, uploadJobStatusResponse => {
      uploadJobStatusResponse.copy(finished = time.some, status = CompleteJobStatus)
    })
  }

}
