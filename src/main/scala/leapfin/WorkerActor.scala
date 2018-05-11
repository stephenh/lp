package leapfin

import akka.actor.Actor
import akka.event.Logging
import scala.util.Random

object WorkerActor {
  val matchString = "Lpfn"
}

// Should move/extract this
class CountingStream[T](other: Stream[T]) {
  // Side effects are sneaky, but watch bytes go by to count them
  var read = 0
  // I'd prefer to extend the Stream trait, for using composition for now
  val stream = other.map { char =>
    read += 1
    char
  }
}

class WorkerActor extends Actor {

  private val log = Logging(context.system, this)
  private val stream = new CountingStream(Random.alphanumeric)

  override def receive = {
    case StartWork(expiration) =>
      // It's not super-kosher to block in an actor, so we could have this
      // worker actor spin up a thread dedicated to reading the random bytes,
      // but given we don't need to listen to any more messages from our driver
      // until we're done, I'm going to just block this for now. As long as
      // akka system was >10 threads, should be fine.

      // TODO: Replace the now with something more functional.
      // TODO: Use a clock so we are more testeable. Haven't done Akka before so need to google DI.
      var now = System.currentTimeMillis()
      val words = stream.stream.sliding(WorkerActor.matchString.length)
      // Doing Stream.find(word) would be nice, but not sure how to make it check the time
      // after every word. Oh. Could have a timed stream. That would be cute. TODO.
      var found = false
      do {
        val word = new String(words.next().toArray)
        if (word == WorkerActor.matchString) {
          found = true
        }
        now = System.currentTimeMillis()
      } while (now < expiration && !found)
      // Reply
      if (found) {
        sender() ! WorkDone("SUCCESS", now - expiration, stream.read)
      }  else {
        // Going to report time taken/bytesRead
        sender() ! WorkDone("TIMEOUT", now - expiration, stream.read)
      }
      // TODO Research how akka handles exceptions in our receive block,
      // e.g. the parent? someone is notified.
    case m =>
      log.info(s"Unknown message ${m}")
  }
}