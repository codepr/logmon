package com.logmon.actors

import akka.actor.{Props, Actor, ActorLogging}

object StatsActor {
  final case class LogAggregated(
      hits: Int,
      routes: (String, Int),
      statusCodes: (Int, Int)
  )
  final case class LogAlarm(throughput: Int, threshold: Int)
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
