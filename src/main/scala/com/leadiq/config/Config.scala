package com.leadiq.config

import cats.effect.{ContextShift, Sync}
import io.chrisdavenport.linebacker.Linebacker
import pureconfig.module.catseffect.loadConfigF
import pureconfig.generic.auto._

final case class Server(host: String, port: Int, version: String)

final case class Job(cron: String, maxParallelTasks: Int)

final case class Connection(maxTotalConnections: Int, maxWaitQueueLimit: Int, responseHeaderTimeout: Int)

final case class Config(clientId: String, imgurApi: String, connection: Connection, server: Server, job: Job)

object Config {

  def load[F[_]: Sync: ContextShift](implicit linebacker: Linebacker[F]): F[Config] =
    linebacker.blockCS(loadConfigF[F, Config])
}
