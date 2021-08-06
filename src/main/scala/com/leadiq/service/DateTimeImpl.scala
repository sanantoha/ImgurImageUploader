package com.leadiq.service

import java.time.{Instant, LocalDateTime, ZoneId}
import java.util.concurrent.TimeUnit

import cats.Functor
import cats.syntax.functor._
import cats.effect.Clock
import com.leadiq.algebra.DateTime

class DateTimeImpl[F[_]: Functor: Clock] extends DateTime[F] {

  def now: F[LocalDateTime] = {
    Clock[F].realTime(TimeUnit.MILLISECONDS).map { longValue =>
      LocalDateTime.ofInstant(Instant.ofEpochMilli(longValue), ZoneId.systemDefault)
    }
  }
}
