package leapfin

import akka.actor.Actor
import akka.event.Logging

class DriverActor extends Actor {
  private val log = Logging(context.system, this)
  
  override def receive = {
    case m => log.info(s"Unknown message ${m}")
  }
}