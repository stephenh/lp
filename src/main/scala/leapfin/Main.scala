package leapfin

import akka.actor.ActorSystem
import akka.actor.Props
import java.time.Duration
import java.time.Instant

object Main {
  def main(args: Array[String]): Unit = {
    if (args == Array("-h")) {
      println("Usage: ./example [timeout-in-seconds]")
      sys.exit(1)
    }
    
    // Pretty naive arg parsing
    val duration = if (args.isEmpty) Duration.ofSeconds(60) else Duration.ofSeconds(args(0).toLong)

    // TODO Add config to ensure we have 11 threads (see caveat in WorkerActor that they
    // currently block, which is okay? Not sure yet.). I think the default threads is 10.
    val system = ActorSystem("leapfin")
    
    // TODO use a clock
    val now = Instant.now()
    val expiration = now.plus(duration)

    // The driver will terminate the akka system when done
    val driver = system.actorOf(Props[DriverActor], "driver")
    driver ! StartDriver(now, expiration)
  }
}
