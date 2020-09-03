/**
  * Aggreagate some stats from the log stream, like top routes hit or the max
  * number of status code etc.
  */
package com.logmon.actors

import akka.actor.{Props, Actor, ActorLogging, ActorRef}
import com.logmon.HttpLogParser.LogRecord
import com.logmon.actors.StatsActor

object AggregatorActor {
  final case class PutLogRecord(record: LogRecord)
  final case class LogStats(logger: ActorRef)

  def props(): Props = Props(classOf[AggregatorActor])
}

class AggregatorActor extends Actor with ActorLogging {

  override def receive: Actor.Receive =
    hitsPerSection(Map[String, Int]().empty, Map[Int, Int]().empty)

  private def hitsPerSection(
      routeHits: Map[String, Int],
      statusHits: Map[Int, Int]
  ): Receive = {
    case AggregatorActor.PutLogRecord(record) => {
      val routeCount = routeHits.getOrElse(record.route, 0) + 1
      val statusCount = statusHits.getOrElse(record.statusCode, 0) + 1
      context become hitsPerSection(
        routeHits + (record.route -> routeCount),
        statusHits + (record.statusCode -> statusCount)
      )
    }
    case AggregatorActor.LogStats(logger) => {
      try {
        val totalHits = routeHits.map(_._2).sum
        val (route, hits) = routeHits maxBy (_._2)
        val (status, sHits) = statusHits maxBy (_._2)
        logger ! StatsActor.LogAggregated(
          totalHits,
          (route, hits),
          (status, sHits)
        )
      } catch {
        case _: UnsupportedOperationException => log.info("No records tracked")
      }
    }
  }
}
