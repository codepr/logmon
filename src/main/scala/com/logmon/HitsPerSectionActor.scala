package com.logmon

import akka.actor.{Props, Actor, ActorLogging}

object HitsPerSectionActor {
  final case class Section(section: String)
  final case class SectionCount()

  def props(): Props = Props(classOf[HitsPerSectionActor])
}

class HitsPerSectionActor extends Actor with ActorLogging {

  override def receive: Actor.Receive = hitsPerSection(Map[String, Int]().empty)

  private def hitsPerSection(sectionHits: Map[String, Int]): Receive = {
    case HitsPerSectionActor.Section(section) => {
      val count = sectionHits.getOrElse(section, 0) + 1
      context become hitsPerSection(sectionHits + (section -> count))
    }
    case HitsPerSectionActor.SectionCount() => log.info("{}", sectionHits maxBy(_._2))
  }
}
