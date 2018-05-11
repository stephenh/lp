package leapfin

/**
 * Watches {@code T} (e.g. bytes) get taken out of {@link other} and counts them.
 * 
 * I'd prefer to extend the Stream trait, but Stream is sealed, for using composition
 * for now. Haven't looked into if StreamLike exists.
 */
class CountingStream[T](other: Stream[T]) {
  // Assuming this is used thread-safely, otherwise need volatile/AtomicLong
  var read = 0
  val stream = other.map { i =>
    read += 1
    i
  }
}
