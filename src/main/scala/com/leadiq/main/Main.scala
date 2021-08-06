package com.leadiq.main

import cats.effect._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.temp.par.Par
import com.leadiq.config.Config
import com.leadiq.domain.UploadJobStatusResponse
import com.leadiq.service._
import io.chrisdavenport.linebacker.Linebacker
import io.chrisdavenport.linebacker.contexts.Executors
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.dsl.io._
import org.http4s.server.blaze.BlazeServerBuilder
import cats.effect.syntax.concurrent._
import cats.syntax.applicative._
import scala.collection.concurrent.TrieMap
import scala.concurrent.duration._


object Main extends IOApp {

  def runApp[F[_]: ConcurrentEffect: ContextShift: Par: Timer](): F[ExitCode] = {
    Executors.unbound[F].map(Linebacker.fromExecutorService[F]).use {
      implicit linebacker: Linebacker[F] =>

        for {

          config <- Config.load[F]

          logger <- Slf4jLogger.create

          jobStore = new KVStoreImpl[F, UploadJobStatusResponse](TrieMap.empty[String, UploadJobStatusResponse])
          linkStore = new KVStoreImpl[F, String](TrieMap.empty[String, String])

          res <- BlazeClientBuilder[F](linebacker.blockingContext)
            .withMaxTotalConnections(config.connection.maxTotalConnections)
            .withResponseHeaderTimeout(config.connection.responseHeaderTimeout.seconds)
            .withMaxWaitQueueLimit(config.connection.maxWaitQueueLimit)
            .resource
            .use { client =>

            for {
              loader <- new ImgLoaderImpl[F](client, logger).pure[F]
              uploader = new ImgurUploaderImpl[F](config, client, logger)

              dateTime = new DateTimeImpl[F]()

              imgurUploaderImpl = new ImgurImageUploaderImpl[F](jobStore, linkStore, dateTime, logger)
              http = new ImgurImageUploaderHttp[F](config, imgurUploaderImpl)

              jobTaskScheduler = new JobTaskSchedulerImpl[F](config, loader, uploader, jobStore, linkStore, dateTime, logger)
              _ <- jobTaskScheduler.run().start

              _ <- BlazeServerBuilder[F].bindHttp(config.server.port, config.server.host)
                .withHttpApp(http.service.orNotFound).serve.compile.drain

            } yield ExitCode.Success
          }
        } yield res
    }
  }

  override def run(args: List[String]): IO[ExitCode] = runApp[IO]()

}