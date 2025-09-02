package expo.modules.calendar.next

import expo.modules.calendar.CalendarUtils
import java.util.Calendar

object CalendarNextUtils {
  val sdf = CalendarUtils.sdf

  fun dateToMilliseconds(stringValue: String?): Long? {
    if (stringValue == null) {
      return null
    }

    return try {
      val parsedDate = sdf.parse(stringValue)
        ?: return null

      val cal = Calendar.getInstance().apply {
        time = parsedDate
      }
      cal.timeInMillis
    } catch (e: Exception) {
      null
    }
  }

  fun dateToString(longValue: Long?): String? {
    if (longValue == null) {
      return null
    }
    val cal = Calendar.getInstance().apply {
      timeInMillis = longValue
    }

    return sdf.format(cal.time)
  }
}
