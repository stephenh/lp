package leapfin

import java.time.Instant
import scala.util.{ Failure, Success, Try }
import java.time.Clock

/**
 * Returns items from {@code source} until we hit an expiration time.
 */
object TimedStream {
  def readUntilExpired[T](source: Stream[T], clock: Clock, expiration: Instant): Stream[Try[T]] = {
    source.map { i =>
      // It's expensive to check time on every single char, maybe we should only check on batches?
      // It's also likely expensive to wrap every stream value with Success, would need to profile.
      if (clock.instant().isAfter(expiration)) {
        Failure(new RuntimeException("Timeout"))
      } else {
        Success(i)
      }
    }
  }
}