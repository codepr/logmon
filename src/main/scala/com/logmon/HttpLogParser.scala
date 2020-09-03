package com.logmon

import scala.util.matching.Regex
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter._

object HttpLogParser {
  final case class LogRecord(
    addr: String,
    dateTime: java.time.LocalDateTime,
    method: String,
    route: String,
    statusCode: Int,
    bytes: Int
  )

  private val logRegex = """^(\S+) - - \[(.+?)\] \"(\S+) (\S+) \S+\/\S+\" (\S+) (\S+)""".r // Regex pattern to parse the log
  private val formatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z");

  def parse(line: String): Option[LogRecord] = {
    line match {
      case logRegex(addr, dt, method, route, statusCode, bytes) =>
        Some(LogRecord(
          addr,
          LocalDateTime.parse(dt, formatter),
          method,
          route,
          statusCode.toInt,
          bytes.toInt
        ))
      case _ => None
    }
  }
}
