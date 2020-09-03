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
import com.logmon.StatsActor._
import com.logmon.FlowCheckActor._
import HttpLogParser._
import scala.language.postfixOps

object LogMonitor extends App {
  val path: Path = Paths.get("access.log")

  val chunkSize = 8192
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val meanHitsActor = system.actorOf(FlowCheckActor.props(120, 30))
  val hitsPerSection = system.actorOf(StatsActor.props())

  import system.dispatcher

  system.scheduler.schedule(
    1 second,
    1 second,
    meanHitsActor,
    FlowCheckActor.MeanHitsPerSecond
  )

  system.scheduler.schedule(
    10 second,
    10 second,
    hitsPerSection,
    StatsActor.GetStats
  )

  val tailSource: Source[ByteString, NotUsed] = FileTailSource(
    path = path,
    maxChunkSize = chunkSize,
    startingPosition = 0,
    pollingInterval = 500 millis
  )

  tailSource
    .via(Framing.delimiter(ByteString("\n"), chunkSize))
    .map(_.utf8String)
    .runForeach(line => {
      meanHitsActor ! FlowCheckActor.Hit
      HttpLogParser.parse(line) map (r =>
        hitsPerSection ! StatsActor.PutLogRecord(r)
      )
    })
}
