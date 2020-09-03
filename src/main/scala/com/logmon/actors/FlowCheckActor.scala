/**
  * Monitor the flow of the stream, calculating the average of hits in a
  * defined size window in the past (e.g. last 5 mins) and if the value
  * surpass a given tolerance, trigger an alarm and communicate it to the
  * `StatsActor`.
  *
  * Communicate a recover alarm if the flow get below the alarm threshold
  * again.
  */
package com.logmon.actors

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.actor.Props
import com.logmon.actors.StatsActor

object FlowCheckActor {
  // Just a hit to be recorded as an Integer counter
  final case object Hit
  /*
   * Calculate average on last N points and reset the counter.
   * To be run once every defined time (e.g. 1s)
   */
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
      // Basic check for the size of the window, trim the list by removing
      // oldest values
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
