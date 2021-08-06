package com.leadiq.domain

import cats.Show

sealed abstract class ImgUpError(val msg: String) extends Throwable(msg) with Product with Serializable

object ImgUpError {

  implicit val cbrError: Show[ImgUpError] = Show.fromToString[ImgUpError]

  final case class WrongUrl(override val msg: String) extends ImgUpError(msg)
  final case class WrongCroneExpression(override val msg: String) extends ImgUpError(msg)
  final case class ImgurErrorResponse(override val msg: String) extends ImgUpError(msg)
}

