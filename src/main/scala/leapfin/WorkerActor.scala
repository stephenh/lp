package leapfin

import akka.actor.Actor
import akka.event.Logging
import java.time.Clock
import scala.util.{ Failure, Random, Success }
import java.time.Duration

object WorkerActor {
  val matchString = "Lpfn"
}

/**
 * Scans a random stream for the characters Lpfn.
 *
 * Currently blocks the actor thread while looking.
 * 
 * It's not super-kosher to block an actor, as we can't receive any
 * messages while doing so. So we could have this worker actor spin up
 * a thread dedicated to reading the random bytes, but not managing
 * threads is a perk of Akka, and our current specs don't require any
 * other driver messages once we start searching, so I'm going to just
 * this for now.
 * 
 * As long as akka system was >10 threads, should be fine.
 */
class WorkerActor extends Actor {

  private val log = Logging(context.system, this)
  // TODO: Accept clock via constructor for testing
  private val clock = Clock.systemUTC()
  // TODO: Accept a random source stream via constructor so non-random source
  // streams can be passed in for deterministic unit tests.
  private val random = Random.alphanumeric

  override def receive = {
    case StartWork(start, expiration) =>
      // Creating a stream of random chars that is:
      // a) Counted so we remember how many raw chars we took
      // b) Sliding so we see groups of 4 at a time
      // c) Timed, so we start getting Failure after expiration has past
      val counted = new CountingStream(random)
      val words = counted.stream.sliding(WorkerActor.matchString.length).map { chars => new String(chars.toArray) }
      val timed = TimedStream.readUntilExpired(words.toStream, clock, expiration)

      // Now keep looking (blocking) until we find what we want or timeout
      val result = timed.find {
        case Success(WorkerActor.matchString) => true // found it
        case Failure(e) => true // timed out
        case Success(_) => false // keep looking
      }

      // Reply
      val found = result == Some(Success(WorkerActor.matchString))
      // TODO Research how akka handles exceptions in our receive block, a big try/catch seems naive?
      val status = if (found) "SUCCESS" else "TIMEOUT"
      val duration = Duration.between(start, clock.instant()).toMillis()
      sender() ! WorkDone(status, duration, counted.read)
    case m =>
      log.info(s"Unknown message ${m}")
  }
}