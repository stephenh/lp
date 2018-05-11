package leapfin

case class StartWork(expiration: Long)

case class WorkDone(status: String, timeTaken: Long, bytesRead: Long)
