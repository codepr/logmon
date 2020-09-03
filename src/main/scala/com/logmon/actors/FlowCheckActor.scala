package com.logmon.actors

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.actor.Props
import com.logmon.actors.StatsActor

object FlowCheckActor {
  final case object Hit
  final case class MeanHitsPerSecond(logger: ActorRef)

  def props(period: Int, alertThreshold: Int): Props =
    Props(new FlowCheckActor(period, alertThreshold))
}

class FlowCheckActor(period: Int, alertThreshold: Int)
    extends Actor
    with ActorLogging {

  override def receive = meanHits(List().empty, 0, false)

  private def meanHits(
      hitsWindow: List[Int],
      hits: Int,
      alert: Boolean
  ): Receive = {
    case FlowCheckActor.Hit =>
      context become meanHits(hitsWindow, hits + 1, alert)
    case FlowCheckActor.MeanHitsPerSecond(logger) => {
      val window = if (hitsWindow.length == period) {
        (hits :: hitsWindow).drop(1)
      } else {
        hits :: hitsWindow
      }
      val mean = window.sum / window.length
      // log.info("Mean throughput (last 120s): {} msg/s ", mean)
      val alertVal = if (!alert && mean > alertThreshold) {
        logger ! StatsActor.LogAlarm(mean, alertThreshold)
        true
      } else if (alert && mean < alertThreshold) {
        logger ! StatsActor.LogRecover(mean, alertThreshold)
        false
      } else {
        alert
      }
      context become meanHits(window, 0, alertVal)
    }
  }
}
