package com.leadiq.domain

import java.time.LocalDateTime

import cats.Show

final case class Uploaded(pending: List[String], complete: List[String], failed: List[String])

final case class UploadJobStatusResponse(id: String,
                                         created: LocalDateTime,
                                         finished: Option[LocalDateTime],
                                         status: String,
                                         uploaded: Uploaded)

object UploadJobStatusResponse {
  implicit val uploadJobStatusResponseShow: Show[UploadJobStatusResponse] = Show.fromToString[UploadJobStatusResponse]
}
