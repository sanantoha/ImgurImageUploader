package com.leadiq.algebra

import com.leadiq.domain.ImgurUploadResponse

trait ImgurUploader[F[_]] {

  def upload(array: Array[Byte]): F[ImgurUploadResponse]
}
