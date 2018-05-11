package leapfin

import java.time.Instant
import java.time.Duration

case class StartDriver(start: Instant, expiration: Instant)

case class StartWork(start: Instant, expiration: Instant)

// TODO: Use a status enum
case class WorkDone(status: String, timeTaken: Long, bytesRead: Long)
