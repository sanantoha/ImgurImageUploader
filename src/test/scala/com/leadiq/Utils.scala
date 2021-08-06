package com.leadiq

import java.time.LocalDateTime

import cats.effect.Clock
import cats.effect.concurrent.Ref
import cats.instances.option._
import cats.syntax.applicative._
import cats.syntax.apply._
import cats.syntax.functor._
import cats.syntax.option.none
import cats.syntax.traverse._
import cats.{Applicative, Functor, Monad, Show}
import com.leadiq.algebra.{DateTime, ImgLoader, ImgurUploader, KVStore}
import com.leadiq.domain.{ImgurUploadResponse, UploadJobStatusResponse, Uploaded}


object Utils {

  val ErrorUrl = "Invalid URI: Invalid input ' ', expected '+', '.', ':', SubDelims, '-', Unreserved, Digit, Alpha or " +
    "PctEncoded (line 1, column 6):\nwrong url\n     ^"

  val dummyUploadJobStatusResponse = UploadJobStatusResponse("", LocalDateTime.now, none, "", Uploaded(Nil, Nil, Nil))

  def mkDateTime[F[_]: Applicative: Clock](localDateTime: LocalDateTime): DateTime[F] = new DateTime[F] {
    override def now: F[LocalDateTime] = localDateTime.pure[F]
  }


  def mkLoader[F[_]: Functor](ref: Ref[F, String], result: Array[Byte]): ImgLoader[F] = new ImgLoader[F] {
    override def load(url: String): F[Array[Byte]] = {
      ref.set(url) as result
    }
  }

  def mkUploader[F[_]: Functor](ref: Ref[F, Array[Byte]], result: ImgurUploadResponse): ImgurUploader[F] = new ImgurUploader[F] {
    override def upload(arr: Array[Byte]): F[ImgurUploadResponse] = ref.set(arr) as result
  }

  def mkKVStore[F[_]: Monad, V: Show](refKey: Ref[F, String], refVal: Ref[F, V], result: List[V]): KVStore[F, V] =
    new KVStore[F, V] {
      override def get(key: String): F[Option[V]] = refKey.set(key) as result.headOption

      override def put(key: String, value: V): F[Unit] = refKey.set(key) *> refVal.set(value)

      override def update(key: String, f: V => V): F[Unit] =
        refKey.set(key) <* result.headOption.map(v => refVal.set(v) as f(v)).sequence

      override def values(): F[List[V]] = result.pure[F]
    }
}
