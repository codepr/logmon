package com.logmon

import org.scalatest._
import flatspec._
import matchers._
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.logmon.HttpLogParser
import com.logmon.HttpLogParser.LogRecord

class HttpLogParserSpec extends AnyFlatSpec with should.Matchers {
  "An HttpLogParser" should "parse log lines" in {
    val line =
      "199.72.81.55 - - [01/Jul/1995:00:00:15 -0400] \"POST /login HTTP/1.0\" 401 1420"
    val formatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z")
    val dateTime = LocalDateTime.parse("01/Jul/1995:00:00:15 -0400", formatter)
    HttpLogParser.parse(line) should be(
      Some(
        LogRecord(
          "199.72.81.55",
          dateTime,
          "POST",
          "/login",
          401,
          1420
        )
      )
    )
  }

  it should "return None if the line doesn't match the Common Log Format" in {
    val line =
      "199.72.81.55 - - \"POST /login HTTP/1.0\" 401 1420"
    HttpLogParser.parse(line) should be(None)
  }
}
