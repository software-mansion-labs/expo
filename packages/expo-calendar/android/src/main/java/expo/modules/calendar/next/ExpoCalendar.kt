package expo.modules.calendar.next

import android.content.ContentValues
import android.database.Cursor
import android.provider.CalendarContract
import android.text.TextUtils
import expo.modules.calendar.CalendarUtils
import expo.modules.calendar.availabilityConstantMatchingString
import expo.modules.calendar.next.records.CalendarRecord
import expo.modules.calendar.next.records.CalendarAccessLevel
import expo.modules.calendar.next.records.AttendeeType
import expo.modules.calendar.next.records.AlarmMethod
import expo.modules.kotlin.AppContext
import expo.modules.kotlin.apifeatures.EitherType
import expo.modules.kotlin.exception.Exceptions
import expo.modules.kotlin.sharedobjects.SharedObject
import java.util.TimeZone

@OptIn(EitherType::class)
class ExpoCalendar : SharedObject {
  val calendarRecord: CalendarRecord?

  constructor(calendar: CalendarRecord) {
    this.calendarRecord = calendar
  }

  constructor(cursor: Cursor) {
    this.calendarRecord = CalendarRecord(
    id = CalendarUtils.optStringFromCursor(cursor, CalendarContract.Calendars._ID),
    title = CalendarUtils.optStringFromCursor(cursor, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME),
    isPrimary = CalendarUtils.optIntFromCursor(cursor, CalendarContract.Calendars.IS_PRIMARY) == 1,
    name = CalendarUtils.optStringFromCursor(cursor, CalendarContract.Calendars.NAME),
    color = CalendarUtils.optIntFromCursor(cursor, CalendarContract.Calendars.CALENDAR_COLOR),
    ownerAccount = CalendarUtils.optStringFromCursor(cursor, CalendarContract.Calendars.OWNER_ACCOUNT),
    timeZone = CalendarUtils.optStringFromCursor(cursor, CalendarContract.Calendars.CALENDAR_TIME_ZONE),
    isVisible = CalendarUtils.optIntFromCursor(cursor, CalendarContract.Calendars.VISIBLE) != 0,
    isSynced = CalendarUtils.optIntFromCursor(cursor, CalendarContract.Calendars.SYNC_EVENTS) != 0,
    allowsModifications = CalendarUtils.optIntFromCursor(cursor, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL) == CalendarContract.Calendars.CAL_ACCESS_ROOT ||
      CalendarUtils.optIntFromCursor(cursor, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL) == CalendarContract.Calendars.CAL_ACCESS_OWNER ||
      CalendarUtils.optIntFromCursor(cursor, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL) == CalendarContract.Calendars.CAL_ACCESS_EDITOR ||
      CalendarUtils.optIntFromCursor(cursor, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL) == CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR,
    accessLevel = CalendarUtils.optStringFromCursor(cursor, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)?.let { accessLevelString ->
      try {
        CalendarAccessLevel.values().find { it.value == accessLevelString } ?: CalendarAccessLevel.NONE
      } catch (e: Exception) {
        CalendarAccessLevel.NONE
      }
    } ?: CalendarAccessLevel.NONE,
    allowedReminders = CalendarUtils.optStringFromCursor(cursor, CalendarContract.Calendars.ALLOWED_REMINDERS)?.split(",")?.filter { it.isNotEmpty() }?.mapNotNull { reminderString ->
      try {
        AlarmMethod.values().find { it.value == reminderString } ?: AlarmMethod.DEFAULT
      } catch (e: Exception) {
        AlarmMethod.DEFAULT
      }
    } ?: emptyList(),
    allowedAttendeeTypes = CalendarUtils.optStringFromCursor(cursor, CalendarContract.Calendars.ALLOWED_ATTENDEE_TYPES)?.split(",")?.filter { it.isNotEmpty() }?.mapNotNull { attendeeTypeString ->
      try {
        AttendeeType.values().find { it.value == attendeeTypeString } ?: AttendeeType.NONE
      } catch (e: Exception) {
        AttendeeType.NONE
      }
    } ?: emptyList(),
    )
  }

  fun getEvents(startDate: Any, endDate: Any): List<ExpoCalendarEvent> {
    if (calendarRecord?.id == null) {
      throw Exception("Calendar id is null")
    }
    val contentResolver = (appContext?.reactContext ?: throw Exceptions.ReactContextLost()).contentResolver
    val cursor = CalendarUtils.findEvents(contentResolver, startDate, endDate, listOf(calendarRecord.id ?: ""))
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
    fun saveCalendar(calendarRecord: CalendarRecord, appContext: AppContext): Int {
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

      val values = ContentValues().apply {
        put(CalendarContract.Calendars.NAME, calendarRecord.name)
        put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, calendarRecord.title)
        put(CalendarContract.Calendars.VISIBLE, calendarRecord.isVisible)
        put(CalendarContract.Calendars.SYNC_EVENTS, calendarRecord.isSynced)
        put(CalendarContract.Calendars.ACCOUNT_NAME, source.name)
        put(CalendarContract.Calendars.ACCOUNT_TYPE, if (source.isLocalAccount) CalendarContract.ACCOUNT_TYPE_LOCAL else source.type)
        put(CalendarContract.Calendars.CALENDAR_COLOR, calendarRecord.color)
        put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, calendarRecord.accessLevel?.let { accessLevel ->
          when (accessLevel) {
            CalendarAccessLevel.OWNER -> CalendarContract.Calendars.CAL_ACCESS_OWNER
            CalendarAccessLevel.EDITOR -> CalendarContract.Calendars.CAL_ACCESS_EDITOR
            CalendarAccessLevel.CONTRIBUTOR -> CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR
            CalendarAccessLevel.READ -> CalendarContract.Calendars.CAL_ACCESS_READ
            CalendarAccessLevel.RESPOND -> CalendarContract.Calendars.CAL_ACCESS_RESPOND
            CalendarAccessLevel.FREEBUSY -> CalendarContract.Calendars.CAL_ACCESS_FREEBUSY
            CalendarAccessLevel.OVERRIDE -> CalendarContract.Calendars.CAL_ACCESS_OVERRIDE
            CalendarAccessLevel.ROOT -> CalendarContract.Calendars.CAL_ACCESS_ROOT
            CalendarAccessLevel.NONE -> CalendarContract.Calendars.CAL_ACCESS_NONE
          }
        } ?: CalendarContract.Calendars.CAL_ACCESS_OWNER)
        put(CalendarContract.Calendars.OWNER_ACCOUNT, source.name)

        if (calendarRecord.timeZone != null) {
          put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, calendarRecord.timeZone)
        } else {
          put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().id)
        }

        if (calendarRecord.allowedAvailabilities.isNotEmpty()) {
          val availabilityValues = calendarRecord.allowedAvailabilities.map { availability ->
            availabilityConstantMatchingString(availability)
          }
          if (availabilityValues.isNotEmpty()) {
            put(CalendarContract.Calendars.ALLOWED_AVAILABILITY, TextUtils.join(",", availabilityValues))
          }
        }

        if (calendarRecord.allowedReminders.isNotEmpty()) {
          put(CalendarContract.Calendars.ALLOWED_REMINDERS, TextUtils.join(",", calendarRecord.allowedReminders.map { it.value }))
        }

        if (calendarRecord.allowedAttendeeTypes.isNotEmpty()) {
          put(CalendarContract.Calendars.ALLOWED_ATTENDEE_TYPES, TextUtils.join(",", calendarRecord.allowedAttendeeTypes.map { it.value }))
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

      val calendarId = calendarUri?.lastPathSegment!!.toInt()
      return calendarId
    }
  }

}