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
import com.logmon.HitsPerSectionActor._
import com.logmon.MeanHitsPerWindowActor._
import scala.language.postfixOps

object HttpLogParser {
  def extractSection(line: String): String = line.split(" ")(6)
}

object LogMonitor extends App {
  val path: Path = Paths.get("access.log")

  val maxLineSize = 8192
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val meanHitsActor = system.actorOf(MeanHitsPerWindowActor.props(120, 30))
  val hitsPerSection = system.actorOf(HitsPerSectionActor.props())

  import system.dispatcher

  system.scheduler.schedule(
    1 second,
    1 second,
    meanHitsActor,
    MeanHitsPerWindowActor.MeanHitsPerSecond()
  )

  system.scheduler.schedule(
    10 second,
    10 second,
    hitsPerSection,
    HitsPerSectionActor.SectionCount()
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
    .runForeach(line => {
      meanHitsActor ! MeanHitsPerWindowActor.Hit(line)
      hitsPerSection ! HitsPerSectionActor.Section(HttpLogParser.extractSection(line))
    })
}
