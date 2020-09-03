package com.logmon.actors

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActors, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import com.logmon.actors.FlowCheckActor
import com.logmon.actors.StatsActor

class FlowCheckActorSpec()
    extends TestKit(ActorSystem("FlowCheckActorSpec"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A FlowCheckActor actor" must {

    "trigger an alarm by sending it to a logger actor if flow is too high" in {
      val probe = TestProbe()
      val flow = system.actorOf(FlowCheckActor.props(10, 5))
      // Let's add some hits
      for (i <- 1 to 10) {
        flow ! FlowCheckActor.Hit
      }
      flow ! FlowCheckActor.MeanHitsPerSecond(probe.ref)
      probe.expectMsg(StatsActor.LogAlarm(10, 5))
    }

    "signal a recover of alarm if the throughput re-enter below the threshold" in {
      val probe = TestProbe()
      val flow = system.actorOf(FlowCheckActor.props(10, 5))
      // Let's add some hits
      for (i <- 1 to 10) {
        flow ! FlowCheckActor.Hit
      }
      flow ! FlowCheckActor.MeanHitsPerSecond(probe.ref)
      probe.expectMsg(StatsActor.LogAlarm(10, 5))
      for (i <- 1 to 3) {
        flow ! FlowCheckActor.MeanHitsPerSecond(probe.ref)
      }
      flow ! FlowCheckActor.MeanHitsPerSecond(probe.ref)
      probe.expectMsg(StatsActor.LogRecover(3, 5))
    }
  }
}
