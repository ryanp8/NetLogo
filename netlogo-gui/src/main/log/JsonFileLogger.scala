// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.log

import java.io.{ File, FilenameFilter, FileWriter, PrintWriter }
import java.nio.file.Path
import java.time.LocalDateTime

import collection.JavaConverters._

import org.json.simple.JSONValue

class JsonFileLogger(private val logDirectoryPath: Path) extends FileLogger {

  val fileNameFilter = new FilenameFilter {
    override def accept(dir: File, name: String) = {
      name.startsWith("netlogo_log_") && name.endsWith(".json")
    }
  }

  private val _writer = {
    val now         = LocalDateTime.now
    val logFileName = s"netlogo_log_${now.format(DateTimeFormats.file)}.json"
    val logFilePath = logDirectoryPath.resolve(logFileName)
    val logFile     = logFilePath.toFile()
    new PrintWriter(new FileWriter(logFile))
  }
  this._writer.write("[\n")

  private var _first = true

  override def log(event: String, eventInfo: Map[String, Any]) {
    if (this._first) {
      this._first = false
      this._writer.write("  ")
    } else {
      this._writer.write(", ")
    }

    val timeStamp = LocalDateTime.now

    val map = Map[String, Any](
      "event"     -> event
    , "timeStamp" -> timeStamp.format(DateTimeFormats.logEntry)
    )
    val finalMap = if (!eventInfo.isEmpty) {
      map + ("eventInfo" -> eventInfo.asJava)
    } else {
      map
    }
    JSONValue.writeJSONString(finalMap.asJava, this._writer)
    this._writer.write("\n")
  }

  override def close() {
    this._writer.write("]")
    this._writer.flush()
    this._writer.close()
  }

}
