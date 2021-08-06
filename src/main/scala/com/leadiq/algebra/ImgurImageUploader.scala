package com.leadiq.algebra

import com.leadiq.domain.{UploadJobStatusResponse, UploadRequest, UploadResponse, UploadedLinksResponse}

trait ImgurImageUploader[F[_]] {

  def jobStatus(jobId: String): F[Option[UploadJobStatusResponse]]

  def allUploadedLinks(): F[UploadedLinksResponse]

  def uploadImages(uploadRequest: UploadRequest): F[UploadResponse]
}
