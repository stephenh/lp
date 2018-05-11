package leapfin

import akka.actor.Actor
import akka.actor.Props
import akka.event.Logging
import java.time.Duration

class DriverActor extends Actor {

  private val log = Logging(context.system, this)
  // Seems okay to create all 10 children up front
  private val children = Range(0, 10).map { i =>
    context.actorOf(Props[WorkerActor], name = s"worker-${i}")
  }
  private val results = scala.collection.mutable.Buffer[WorkDone]()
  private var duration: Duration = null
  
  override def receive = {
    case StartDriver(now, expiration) =>
      this.duration = Duration.between(now, expiration)
      children.foreach { ref =>
        // For now going to trust WorkerActor to respect the timeout; could
        // add scheduled messages to check-in on it and kill/return if needed
        log.info(s"Starting ${ref}")
        ref ! StartWork(now, expiration)
      }
    case m @ WorkDone(status, timeTaken, bytesRead) =>
      results += m
      if (results.size == children.size) {
        // Still haven't read about akka testing, so extracting to an object for testing
        println(DriverActor.printResults(results, duration).mkString("\n"))
        context.system.terminate()
      }
    case m =>
      log.info(s"Unknown message ${m}")
  }
}

object DriverActor {
  def printResults(results: Seq[WorkDone], duration: Duration): Seq[String] = {
    // Output in descending order, which means longest ones first
    val lines = results.sortBy(_.timeTaken)(Ordering[Long].reverse).map { r => 
      if (r.status == "SUCCESS") {
        s"SUCCESS took ${r.timeTaken} millis read ${r.bytesRead} bytes"
      } else {
        // TODO haven't captured the error/stack trace, but would go to stderr
        r.status
      }
    }
    val successBytes = results.filter(_.status == "SUCCESS").map(_.bytesRead).sum
    val successTime = results.filter(_.status == "SUCCESS").map(_.timeTaken).sum
    val seconds = Duration.ofMillis(successTime).getSeconds
    val successBytesPerSec = if (seconds == 0) "N/A" else successBytes.toDouble / seconds
    lines ++ Seq(s"Average success bytes/second ${successBytesPerSec}")
  }
  
}