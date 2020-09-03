package com.logmon

import akka.actor.{Actor, ActorLogging}
import akka.actor.Props

object FlowCheckActor {
  final case object Hit
  final case object MeanHitsPerSecond

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
    case FlowCheckActor.MeanHitsPerSecond => {
      val window = if (hitsWindow.length == period) {
        (hits :: hitsWindow).drop(1)
      } else {
        hits :: hitsWindow
      }
      val mean = window.sum / window.length
      log.info("Mean throughput (last 120s): {} msg/s ", mean)
      val alertVal = if (!alert && mean > alertThreshold) {
        log.warning(
          "Alert: throughput {} msg/s (avg) exceeded threshold {}",
          mean,
          alertThreshold
        )
        true
      } else if (alert && mean < alertThreshold) {
        log.warning(
          "Alert recovered: throughput {} msg/s (avg) below threshold {}",
          mean,
          alertThreshold
        )
        false
      } else {
        alert
      }
      context become meanHits(window, 0, alertVal)
    }
  }
}
