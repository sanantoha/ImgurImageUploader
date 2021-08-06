package com.leadiq.config

import java.util.concurrent.Executors

import cats.effect.{ContextShift, IO}
import io.chrisdavenport.linebacker.contexts.{Executors => E}
import cats.syntax.functor._
import com.leadiq.UnitSpec
import io.chrisdavenport.linebacker.Linebacker

import scala.concurrent.ExecutionContext


class ConfigSpec extends UnitSpec {

  import Config._

  val testEc = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())

  "Config " should "read config" in {
    val ioRes: IO[Config] = E.unbound[IO].map(Linebacker.fromExecutorService[IO]).use {
      implicit linebacker: Linebacker[IO] =>
        implicit val _: ContextShift[IO] = IO.contextShift(testEc)
        load[IO]
    }

    ioRes.unsafeRunSync() shouldBe Config(clientId = "CLIENT_ID", imgurApi = "IMGUR_API",
      connection = Connection(maxTotalConnections = 1, maxWaitQueueLimit = 1, responseHeaderTimeout = 30),
      server = Server(host = "localhost", port = 8080, version = "v1"),
      job = Job(cron = "*/1 * * ? * *", maxParallelTasks = 1)
    )
  }
}
