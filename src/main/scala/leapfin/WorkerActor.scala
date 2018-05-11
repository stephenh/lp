package leapfin

import akka.actor.Actor
import akka.event.Logging
import scala.util.Random
import java.time.Instant

object WorkerActor {
  val matchString = "Lpfn"
}

class WorkerActor extends Actor {

  private val log = Logging(context.system, this)
  private val stream = new CountingStream(Random.alphanumeric)

  override def receive = {
    case StartWork(start, expiration) =>
      // It's not super-kosher to block in an actor, so we could have this
      // worker actor spin up a thread dedicated to reading the random bytes,
      // but given we don't need to listen to any more messages from our driver
      // until we're done, I'm going to just block this for now. As long as
      // akka system was >10 threads, should be fine.

      // TODO: Replace the now with something more functional.
      // TODO: Use a clock so we are more testeable. Haven't done Akka before so need to google DI.
      val words = stream.stream.sliding(WorkerActor.matchString.length)
      // Doing Stream.find(word) would be nice, but not sure how to make it check the time
      // after every word. Oh. Could have a timed stream. That would be cute. TODO.
      var found = false
      do {
        val word = new String(words.next().toArray)
        if (word == WorkerActor.matchString) {
          found = true
        }
      } while (Instant.now().isBefore(expiration) && !found)
      // Reply
      val status = if (found) "SUCCESS" else "TIMEOUT"
      val duration = Instant.now().toEpochMilli() - start.toEpochMilli();
      // Going to report time taken/bytesRead and let driver ignore it
      sender() ! WorkDone(status, duration, stream.read)
    // TODO Research how akka handles exceptions in our receive block,
    // e.g. the parent? someone is notified.
    case m =>
      log.info(s"Unknown message ${m}")
  }
}