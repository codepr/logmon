/**
  * Logger actor, just print some stats about the log stream and alarms when
  * triggered based on some thresholds handled by `FlowCheckActor`. Also print
  * the recover of the alarm if the `FlowCheckActor` detect a slow-down below
  * alert threshold.
  *
  * Could be easily adapted to forward alerts and stats into a queue to some
  * backend services.
  */
package com.logmon.actors

import akka.actor.{Props, Actor, ActorLogging}

object StatsActor {

  /*
   * Contains some minimal stats like total hits counted so far in the log
   * stream, the top hit routes and top status codes
   */
  final case class LogAggregated(
      hits: Int,
      routes: (String, Int),
      statusCodes: (Int, Int)
  )
  /*
   * Print an alarm, based on a fixed threshold when the flow above that value
   */
  final case class LogAlarm(throughput: Int, threshold: Int)
  /*
   * Print a recover alarm, when the flow get below the threshold
   */
  final case class LogRecover(throughput: Int, threshold: Int)

  def props(): Props = Props(classOf[StatsActor])
}

class StatsActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case StatsActor.LogAggregated(hits, routes, statusCodes) =>
      val (route, routeHits) = routes
      val (status, count) = routes
      log.info(
        s"Hits: $hits Top route: $route - $routeHits " +
          s"hits Top status: $status - count count"
      )
    case StatsActor.LogAlarm(throughput, threshold) =>
      log.warning(
        "Alert: throughput {} msg/s (avg) exceeded threshold {}",
        throughput,
        threshold
      )
    case StatsActor.LogRecover(throughput, threshold) =>
      log.warning(
        "Alert recovered: throughput {} msg/s (avg) below threshold {}",
        throughput,
        threshold
      )
  }
}
