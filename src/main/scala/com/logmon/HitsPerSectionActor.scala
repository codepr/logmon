package com.logmon

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object HitsPerSectionActor {
  final case class Hit(line: String)

  def apply(): Behavior[Hit] =
    Behaviors.receive { (context, message) =>
      context.log.info("Hit: {}", message.line)
      Behaviors.same
    }
}
