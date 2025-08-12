package expo.modules.calendar.next

import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.provider.CalendarContract
import android.text.TextUtils
import expo.modules.calendar.CalendarUtils
import expo.modules.calendar.availabilityConstantMatchingString
import expo.modules.calendar.next.records.CalendarRecordNext
import expo.modules.kotlin.AppContext
import expo.modules.kotlin.apifeatures.EitherType
import expo.modules.kotlin.exception.Exceptions
import expo.modules.kotlin.sharedobjects.SharedObject
import java.util.TimeZone

@OptIn(EitherType::class)
class ExpoCalendar : SharedObject {
  val id: String?
  val title: String?
  val isPrimary: Boolean
  val name: String?
  val color: String?
  val ownerAccount: String?
  val timeZone: String?
  val isVisible: Boolean
  val isSynced: Boolean
  val allowsModifications: Boolean
  val cursor: Cursor?
  private val contentResolver
    get() = (appContext?.reactContext ?: throw Exceptions.ReactContextLost()).contentResolver

  constructor(calendar: CalendarRecordNext) {
    this.id = calendar.id
    this.title = calendar.title
    this.isPrimary = calendar.isPrimary
    this.name = calendar.name
    this.color = calendar.color
    this.ownerAccount = null
    this.timeZone = calendar.timeZone
    this.isVisible = calendar.isVisible
    this.isSynced = calendar.isSynced
    this.allowsModifications = calendar.allowsModifications
    this.cursor = null
  }

  constructor(id: String) {
    this.id = id
    this.title = null
    this.isPrimary = true
    this.name = null
    this.color = null
    this.ownerAccount = null
    this.timeZone = null
    this.isVisible = true
    this.isSynced = true
    this.allowsModifications = true
    this.cursor = null
  }

  constructor(cursor: Cursor) {
    this.id = CalendarUtils.optStringFromCursor(cursor, CalendarContract.Calendars._ID)
    this.title = CalendarUtils.optStringFromCursor(cursor, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
    this.isPrimary = CalendarUtils.optIntFromCursor(cursor, CalendarContract.Calendars.IS_PRIMARY) == 1
    this.name = CalendarUtils.optStringFromCursor(cursor, CalendarContract.Calendars.NAME)
    this.color = String.format("#%06X", 0xFFFFFF and CalendarUtils.optIntFromCursor(cursor, CalendarContract.Calendars.CALENDAR_COLOR))
    this.ownerAccount = CalendarUtils.optStringFromCursor(cursor, CalendarContract.Calendars.OWNER_ACCOUNT)
    this.timeZone = CalendarUtils.optStringFromCursor(cursor, CalendarContract.Calendars.CALENDAR_TIME_ZONE)
    this.isVisible = CalendarUtils.optIntFromCursor(cursor, CalendarContract.Calendars.VISIBLE) != 0
    this.isSynced = CalendarUtils.optIntFromCursor(cursor, CalendarContract.Calendars.SYNC_EVENTS) != 0
    this.allowsModifications = CalendarUtils.optIntFromCursor(cursor, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL) == CalendarContract.Calendars.CAL_ACCESS_ROOT ||
      CalendarUtils.optIntFromCursor(cursor, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL) == CalendarContract.Calendars.CAL_ACCESS_OWNER ||
      CalendarUtils.optIntFromCursor(cursor, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL) == CalendarContract.Calendars.CAL_ACCESS_EDITOR ||
      CalendarUtils.optIntFromCursor(cursor, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL) == CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR
    this.cursor = cursor
  }

  fun getEvents(startDate: Any, endDate: Any): List<ExpoCalendarEvent> {
    if (id == null) {
      throw Exception("Calendar id is null")
    }
    val cursor = CalendarUtils.findEvents(contentResolver, startDate, endDate, listOf(id))
    return cursor.use { serializeExpoCalendarEvents(cursor) }
  }

  private fun serializeExpoCalendarEvents(cursor: Cursor): List<ExpoCalendarEvent> {
    val results: MutableList<ExpoCalendarEvent> = ArrayList()
    while (cursor.moveToNext()) {
      results.add(ExpoCalendarEvent(cursor))
    }
    return results
  }
  companion object {
    fun createCalendarNext(calendarRecord: CalendarRecordNext, appContext: AppContext): ExpoCalendar {
      if (calendarRecord.title == null) {
        throw Exception("new calendars require `title`")
      }
      if (calendarRecord.name == null) {
        throw Exception("new calendars require `name`")
      }
      if (calendarRecord.source == null) {
        throw Exception("new calendars require `source`")
      }
      if (calendarRecord.color == null) {
        throw Exception("new calendars require `color`")
      }

      val source = calendarRecord.source!!
      if (source.name == null) {
        throw Exception("new calendars require a `source` object with a `name`")
      }

      val colorValue = when (val color = calendarRecord.color) {
        is String -> {
          val hexColor = color.removePrefix("#")
          try {
            hexColor.toInt(16)
          } catch (e: NumberFormatException) {
            throw Exception("Invalid color format: $color")
          }
        }
        else -> throw Exception("Color must be a string (hex)")
      }

      val values = ContentValues().apply {
        put(CalendarContract.Calendars.NAME, calendarRecord.name)
        put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, calendarRecord.title)
        put(CalendarContract.Calendars.VISIBLE, calendarRecord.isVisible)
        put(CalendarContract.Calendars.SYNC_EVENTS, calendarRecord.isSynced)
        put(CalendarContract.Calendars.ACCOUNT_NAME, source.name)
        put(CalendarContract.Calendars.ACCOUNT_TYPE, if (source.isLocalAccount) CalendarContract.ACCOUNT_TYPE_LOCAL else source.type)
        put(CalendarContract.Calendars.CALENDAR_COLOR, colorValue)
        put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
        put(CalendarContract.Calendars.OWNER_ACCOUNT, source.name)

        if (calendarRecord.timeZone != null) {
          put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, calendarRecord.timeZone)
        } else {
          put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().id)
        }

        if (calendarRecord.allowedAvailabilities.isNotEmpty()) {
          val availabilityValues = calendarRecord.allowedAvailabilities.mapNotNull { availability ->
            availabilityConstantMatchingString(availability)
          }
          if (availabilityValues.isNotEmpty()) {
            put(CalendarContract.Calendars.ALLOWED_AVAILABILITY, TextUtils.join(",", availabilityValues))
          }
        }
      }

      val uriBuilder = CalendarContract.Calendars.CONTENT_URI
        .buildUpon()
        .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
        .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, source.name)
        .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, if (source.isLocalAccount) CalendarContract.ACCOUNT_TYPE_LOCAL else source.type)

      val calendarsUri = uriBuilder.build()
      val contentResolver = (appContext.reactContext ?: throw Exceptions.ReactContextLost()).contentResolver
      val calendarUri = contentResolver.insert(calendarsUri, values)
        ?: throw Exception("Failed to create calendar")

      val calendarId = calendarUri.lastPathSegment!!
      calendarRecord.id = calendarId
      return ExpoCalendar(calendarRecord)
    }
  }

}