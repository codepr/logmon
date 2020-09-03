package com.logmon

import akka.actor.{Props, Actor, ActorLogging}
import com.logmon.HttpLogParser.LogRecord

object HitsPerSectionActor {
  final case class PutLogRecord(record: LogRecord)
  final case object GetStats

  def props(): Props = Props(classOf[HitsPerSectionActor])
}

class HitsPerSectionActor extends Actor with ActorLogging {

  override def receive: Actor.Receive =
    hitsPerSection(Map[String, Int]().empty, Map[Int, Int]().empty)

  private def hitsPerSection(
    routeHits: Map[String, Int],
    statusHits: Map[Int, Int]
  ): Receive = {
    case HitsPerSectionActor.PutLogRecord(record) => {
      val routeCount = routeHits.getOrElse(record.route, 0) + 1
      val statusCount = statusHits.getOrElse(record.statusCode, 0) + 1
      context become hitsPerSection(
        routeHits + (record.route -> routeCount),
        statusHits + (record.statusCode -> statusCount)
      )
    }
    case HitsPerSectionActor.GetStats => {
      try {
        val (route, hits) = routeHits maxBy(_._2)
        val (status, sHits) = statusHits maxBy(_._2)
        log.debug("Max route {}:{} max status {}:{}", route, hits, status, sHits)
      } catch {
        case _: UnsupportedOperationException => log.info("No records tracked")
      }
    }
  }
}
