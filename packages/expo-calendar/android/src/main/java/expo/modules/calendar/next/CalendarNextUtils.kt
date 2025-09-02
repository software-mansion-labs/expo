package expo.modules.calendar.next

import android.content.ContentResolver
import android.database.Cursor
import java.util.Calendar
import java.util.TimeZone
import android.content.ContentUris
import android.provider.CalendarContract
import android.util.Log
import expo.modules.calendar.CalendarModule.Companion.TAG
import expo.modules.calendar.findEventsQueryParameters
import java.text.ParseException
import java.text.SimpleDateFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CalendarNextUtils {
  val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").apply {
    timeZone = TimeZone.getTimeZone("GMT")
  }

  fun dateToMilliseconds(stringValue: String?): Long? {
    if (stringValue == null) return null
    try {
      val cal = Calendar.getInstance()
      val parsedDate = sdf.parse(stringValue)
      cal.time = parsedDate ?: return null
      return cal.timeInMillis
    } catch (_: Exception) {
      return null
    }
  }

  fun dateToString(longValue: Long?): String? {
    if (longValue == null) return null
    val cal = Calendar.getInstance()
    cal.timeInMillis = longValue
    return sdf.format(cal.time)
  }

  fun optStringFromCursor(cursor: Cursor, columnName: String): String? {
    val index = cursor.getColumnIndex(columnName)
    return if (index == -1) {
      null
    } else {
      cursor.getString(index)
    }
  }

  fun stringFromCursor(cursor: Cursor, columnName: String): String {
    val index = cursor.getColumnIndex(columnName)
    if (index == -1) {
      throw Exception("String not found")
    } else {
      return cursor.getString(index)
    }
  }

  fun optIntFromCursor(cursor: Cursor, columnName: String): Int {
    val index = cursor.getColumnIndex(columnName)
    return if (index == -1) {
      0
    } else {
      cursor.getInt(index)
    }
  }

  suspend fun findEvents(contentResolver: ContentResolver, startDate: Any, endDate: Any, calendars: List<String>): Cursor {
    return withContext(Dispatchers.IO) {
      val eStartDate = Calendar.getInstance()
      val eEndDate = Calendar.getInstance()
      try {
        setDateInCalendar(eStartDate, startDate)
        setDateInCalendar(eEndDate, endDate)
      } catch (e: ParseException) {
        Log.e(TAG, "error parsing", e)
      } catch (e: Exception) {
        Log.e(TAG, "misc error parsing", e)
      }
      val uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon()
      ContentUris.appendId(uriBuilder, eStartDate.timeInMillis)
      ContentUris.appendId(uriBuilder, eEndDate.timeInMillis)
      val uri = uriBuilder.build()
      var selection =
        "((${CalendarContract.Instances.BEGIN} >= ${eStartDate.timeInMillis}) " +
          "AND (${CalendarContract.Instances.END} <= ${eEndDate.timeInMillis}) " +
          "AND (${CalendarContract.Instances.VISIBLE} = 1) "
      if (calendars.isNotEmpty()) {
        var calendarQuery = "AND ("
        for (i in calendars.indices) {
          calendarQuery += CalendarContract.Instances.CALENDAR_ID + " = '" + calendars[i] + "'"
          if (i != calendars.size - 1) {
            calendarQuery += " OR "
          }
        }
        calendarQuery += ")"
        selection += calendarQuery
      }
      selection += ")"
      val sortOrder = "${CalendarContract.Instances.BEGIN} ASC"
      val cursor = contentResolver.query(
        uri,
        findEventsQueryParameters,
        selection,
        null,
        sortOrder
      )

      requireNotNull(cursor) { "Cursor shouldn't be null" }
      cursor
    }
  }

  private fun setDateInCalendar(calendar: Calendar, date: Any) {
    when (date) {
      is String -> {
        val parsedDate = sdf.parse(date)
        if (parsedDate != null) {
          calendar.time = parsedDate
        } else {
          Log.e(TAG, "Parsed date is null")
        }
      }

      is Number -> {
        calendar.timeInMillis = date.toLong()
      }

      else -> {
        Log.e(TAG, "date has unsupported type")
      }
    }
  }

  @Throws(SecurityException::class)
  internal fun removeRemindersForEvent(contentResolver: ContentResolver, eventID: Int) {
    val cursor = CalendarContract.Reminders.query(
      contentResolver,
      eventID.toLong(),
      arrayOf(
        CalendarContract.Reminders._ID
      )
    )
    while (cursor.moveToNext()) {
      val reminderUri = ContentUris.withAppendedId(CalendarContract.Reminders.CONTENT_URI, cursor.getLong(0))
      contentResolver.delete(reminderUri, null, null)
    }
  }
}
