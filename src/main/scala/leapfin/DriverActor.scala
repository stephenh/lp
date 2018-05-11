package leapfin

import akka.actor.Actor
import akka.actor.Props
import akka.event.Logging

class DriverActor extends Actor {

  private val log = Logging(context.system, this)
  // Seems okay to create all 10 children up front
  private val children = Range(0, 10).map { i =>
    context.actorOf(Props[WorkerActor], name = s"worker-${i}")
  }
  private val results = scala.collection.mutable.Buffer[WorkDone]()

  
  override def receive = {
    case StartDriver(now, expiration) =>
      children.foreach { ref =>
        // For now going to trust WorkerActor to respect the timeout; could
        // add scheduled messages to check-in on it and kill/return if needed
        log.info(s"Starting ${ref}")
        ref ! StartWork(now, expiration)
      }
    case m @ WorkDone(status, timeTaken, bytesRead) =>
      results += m
      if (results.size == children.size) {
        printResults()
        context.system.terminate()
      }
    case m =>
      log.info(s"Unknown message ${m}")
  }
  
  private def printResults(): Unit = {
    // Output in descending order, which means longest ones first
    results.sortBy(_.timeTaken)(Ordering[Long].reverse).foreach { r => 
      if (r.status == "SUCCESS") {
        println(r) // Just print the whole message, which is lazy to "to spec"
      } else {
        // TODO haven't captured the error/stack trace, but would go to stderr
        println(r.status) // Only print TIMEOUT or FAILURE
      }
    }
    // TODO Need a final line:
    //  A final line of output will show the average bytes read per time unit in a time unit of your choice where failed/timeout workers will not report stats. 11 lines of output total to stdout.
  }
  
}