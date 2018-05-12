package leapfin

import java.time.Duration
import java.util.Arrays
import java.util.concurrent.{ CopyOnWriteArrayList, CountDownLatch, Executors, TimeUnit }
import scala.collection.JavaConverters._
import scala.util.Random

object Main2 {
  val numThreads = 10
  val threadPool = Executors.newScheduledThreadPool(numThreads + 1)
  val search = "Lpfn".chars().toArray()
  // Bizarre, but Random.alphanumeric.take(1).head was not advancing the stream for me,
  // so fallback on alphanumeric's chars[nextInt & chars.length] approach.
  val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

  def main(args: Array[String]): Unit = {
    val duration = Duration.ofSeconds(60)
    val done = new CountDownLatch(numThreads)
    val results = new CopyOnWriteArrayList[WorkDone]()

    val futures = Range(0, numThreads).map { i =>
      threadPool.submit(() => {
        try {
          val random = new Random()
          var found = false
          var read = 0
          val start = System.currentTimeMillis
          val data = new Array[Int](search.length)
          val thread = Thread.currentThread
          while (!found && !thread.isInterrupted) {
            // super ugly but "fast"
            data(0) = data(1)
            data(1) = data(2)
            data(2) = data(3)
            data(3) = chars.charAt(random.nextInt(chars.length))
            read += 1
            found = Arrays.equals(search, data)
          }
          val end = System.currentTimeMillis
          val status = if (found) "SUCCESS" else "TIMEOUT"
          results.add(WorkDone(status, end - start, read))
        } catch {
          case e: Throwable => results.add(WorkDone("FAILURE", 0, 0)) // TODO pass error
        } finally {
          done.countDown()
        }
      })
    }

    // Technically if this runs before any of our threads boot up,
    // those thread's done.countDown() won't happen and our done.await
    // will block.
    val cancelRemaining = threadPool.schedule(
      () => futures.foreach(_.cancel(true)).asInstanceOf[Runnable],
      duration.toMillis,
      TimeUnit.MILLISECONDS)

    done.await()
    cancelRemaining.cancel(true)

    val lines = DriverActor.printResults(results.asScala.toIndexedSeq)
    println(lines.mkString("\n"))

    threadPool.shutdown()
  }
}
