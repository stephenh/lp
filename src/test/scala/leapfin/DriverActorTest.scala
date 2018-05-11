package leapfin

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalactic.source.Position.apply
import org.scalatest.Matchers
import java.time.Duration

@RunWith(classOf[JUnitRunner])
class DriverActorTest extends FunSuite with Matchers {
  val durationOneSecond = Duration.ofSeconds(1)

  test("output with SUCCESS message") {
    printResults(Seq(WorkDone("SUCCESS", 1000, 1))) should be(Seq(
        "SUCCESS took 1000 millis read 1 bytes",
        "Average success bytes/second 1.0"))
  }

  test("output with FAILURE message") {
    printResults(Seq(WorkDone("FAILURE", 1000, 1))) should be(Seq(
        "FAILURE",
        "Average success bytes/second N/A"))
  }

  test("output with TIMEOUT message") {
    printResults(Seq(WorkDone("TIMEOUT", 1000, 1))) should be(Seq(
        "TIMEOUT",
        "Average success bytes/second N/A"))
  }

  test("output should be descending") {
    // given work that came in ascending order
    val in = Seq(WorkDone("SUCCESS", 1000, 1), WorkDone("SUCCESS", 1000, 1))
    // then we output it in descending order
    printResults(in) should be(Seq(
        "SUCCESS took 1000 millis read 1 bytes",
        "SUCCESS took 1000 millis read 1 bytes",
        "Average success bytes/second 1.0"))
  }
  
  private def printResults(work: Seq[WorkDone], duration: Duration = durationOneSecond): Seq[String] = {
    DriverActor.printResults(work, duration)
  }
}
