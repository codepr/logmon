package com.logmon

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.alpakka.file.scaladsl._
import akka.util.{ByteString, Timeout}
import java.io.OutputStream
import java.nio.file.Path
import java.nio.file.Paths
import scala.concurrent._
import scala.concurrent.duration._
import HitsPerSectionActor._
import com.logmon.MeanHitsPerWindowActor._
import scala.language.postfixOps

object LogMonitor extends App {
  val path: Path = Paths.get("access.log")

  val maxLineSize = 8192
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val meanHitsActor = system.actorOf(MeanHitsPerWindowActor.props(120, 30))

  import system.dispatcher

  system.scheduler.schedule(
    Duration.Zero,
    10 second,
    meanHitsActor,
    MeanHitsPerWindowActor.MeanHitsPerSecond()
  )

  val tailSource: Source[ByteString, NotUsed] = FileTailSource(
    path = path,
    maxChunkSize = maxLineSize,
    startingPosition = 0,
    pollingInterval = 500.millis
  )

  tailSource
    .via(Framing.delimiter(ByteString("\n"), 8192))
    .map(_.utf8String)
    .runForeach(line => { meanHitsActor ! MeanHitsPerWindowActor.Hit(line) })
}
