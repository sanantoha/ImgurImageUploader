package com.leadiq.service

import cats.effect._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.leadiq.algebra.ImgLoader
import com.leadiq.domain.ImgUpError.WrongUrl
import io.chrisdavenport.log4cats.Logger
import org.http4s.Uri
import org.http4s.client.Client


class ImgLoaderImpl[F[_]: ConcurrentEffect](client: Client[F], logger: Logger[F]) extends ImgLoader[F] {

  override def load(url: String): F[Array[Byte]] = {
    for {
      uri <- Uri.fromString(url).leftMap(e => WrongUrl(e.getMessage()): Throwable).raiseOrPure
      _ <- logger.info(s"loading url: $uri")
      arr <- client.expect[Array[Byte]](uri)
    } yield arr

  }
}