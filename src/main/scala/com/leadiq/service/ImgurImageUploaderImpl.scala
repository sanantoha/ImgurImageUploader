package com.leadiq.service

import cats.effect.Concurrent
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.option._
import cats.temp.par._
import com.leadiq.algebra._
import com.leadiq.domain._
import io.chrisdavenport.log4cats.Logger


class ImgurImageUploaderImpl[F[_]: Concurrent: Par](jobStore: KVStore[F, UploadJobStatusResponse],
                                                    linkStore: KVStore[F, String],
                                                    dateTime: DateTime[F],
                                                    logger: Logger[F]) extends ImgurImageUploader[F] {

  import ImgurImageUploaderImpl._

  override def jobStatus(jobId: String): F[Option[UploadJobStatusResponse]] = jobStore.get(jobId)

  override def allUploadedLinks(): F[UploadedLinksResponse] = linkStore.values().map(UploadedLinksResponse)

  override def uploadImages(uploadRequest: UploadRequest): F[UploadResponse] = {
    for {
      uuid <- createUuid.pure[F]
      urls = uploadRequest.urls.distinct
      localDateTime <- dateTime.now
      uploadJobStatusResponse = UploadJobStatusResponse(uuid, localDateTime, none, PendingJobStatus, Uploaded(urls, Nil, Nil))
      _ <- logger.info(s"generated new jobId: $uuid for request: $uploadRequest")

      _ <- jobStore.put(uuid, uploadJobStatusResponse)
      _ <- logger.info(s"create new job $uploadJobStatusResponse")

    } yield UploadResponse(uuid)
  }

  def createUuid: String = java.util.UUID.randomUUID.toString
}

object ImgurImageUploaderImpl {

  val PendingJobStatus = "pending"
  val InProgressJobStatus = "in-progress"
  val CompleteJobStatus = "complete"
}