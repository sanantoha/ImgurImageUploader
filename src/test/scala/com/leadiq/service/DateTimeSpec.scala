package com.leadiq.service

import java.time.{LocalDateTime, ZoneId}
import java.util.concurrent.Executors

import cats.Functor
import cats.effect._
import com.leadiq.UnitSpec

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.TimeUnit

class DateTimeSpec extends UnitSpec {

  val testEc = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())
  implicit val cs: ContextShift[IO] = IO.contextShift(testEc)
  val expStartDateTime = LocalDateTime.now

  implicit val clock = new Clock[IO] {
    override def realTime(unit: TimeUnit): IO[Long] =
      IO.pure(expStartDateTime.atZone(ZoneId.systemDefault()).toInstant.toEpochMilli)

    override def monotonic(unit: TimeUnit): IO[Long] = IO.pure(0)
  }

  "DateTime" should "return LocalDateTime" in {
    run[IO]().unsafeRunSync() shouldBe expStartDateTime
  }

  def run[F[_]: Functor: Clock](): F[LocalDateTime] =
    new DateTimeImpl[F].now
}
