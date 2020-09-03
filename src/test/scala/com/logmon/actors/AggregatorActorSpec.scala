package com.logmon.actors

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActors, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import com.logmon.actors.AggregatorActor
import com.logmon.actors.StatsActor
import com.logmon.HttpLogParser

class AggregatorActorSpec()
    extends TestKit(ActorSystem("AggregatorActorSpec"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "An AggregatorActor actor" must {

    "logs collected stats by sending them to a logger actor" in {
      val probe = TestProbe()
      val aggregator = system.actorOf(AggregatorActor.props)
      val record = HttpLogParser.parse(
        "199.72.81.55 - - [01/Jul/1995:00:00:15 -0400] \"POST /login HTTP/1.0\" 401 1420"
      )
      // Let's add some hits
      aggregator ! AggregatorActor.PutLogRecord(record.get)
      aggregator ! AggregatorActor.LogStats(probe.ref)
      probe.expectMsg(StatsActor.LogAggregated(1, ("/login", 1), (401, 1)))
    }

    "aggregate stats like top route and top status code" in {
      val probe = TestProbe()
      val aggregator = system.actorOf(AggregatorActor.props)
      Array(
        "199.72.81.55 - - [01/Jul/1995:00:00:15 -0400] \"POST /login HTTP/1.0\" 401 1420",
        "199.72.81.55 - - [01/Jul/1995:00:00:15 -0400] \"POST /test HTTP/1.0\" 200 1420",
        "199.72.81.55 - - [01/Jul/1995:00:00:15 -0400] \"POST /test HTTP/1.0\" 200 1420",
        "199.72.81.55 - - [01/Jul/1995:00:00:15 -0400] \"POST /test HTTP/1.0\" 200 1420",
        "199.72.81.55 - - [01/Jul/1995:00:00:15 -0400] \"POST /test HTTP/1.0\" 200 1420",
        "199.72.81.55 - - [01/Jul/1995:00:00:15 -0400] \"POST /test HTTP/1.0\" 200 1420",
        "199.72.81.55 - - [01/Jul/1995:00:00:15 -0400] \"POST /login HTTP/1.0\" 401 1420"
      ).map(line =>
        HttpLogParser
          .parse(line)
          .map(record => aggregator ! AggregatorActor.PutLogRecord(record))
      )
      aggregator ! AggregatorActor.LogStats(probe.ref)
      probe.expectMsg(StatsActor.LogAggregated(7, ("/test", 5), (200, 5)))
    }
  }
}
