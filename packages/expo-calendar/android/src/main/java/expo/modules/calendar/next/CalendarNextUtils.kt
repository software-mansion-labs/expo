package expo.modules.calendar.next

import expo.modules.calendar.CalendarUtils
import java.util.Date

object CalendarNextUtils {
  val sdf = CalendarUtils.sdf

  fun dateToMilliseconds(stringValue: String?): Long? {
    if (stringValue == null) return null
    try {
      val parsedDate = sdf.parse(stringValue)
      return parsedDate?.time
    } catch (e: Exception) {
      return null
    }
  }

  fun dateToString(longValue: Long?): String? {
    if (longValue == null) return null
    return sdf.format(Date(longValue))
  }
}
