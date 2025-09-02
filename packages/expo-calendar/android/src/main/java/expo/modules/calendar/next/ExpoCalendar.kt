package expo.modules.calendar.next

import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.provider.CalendarContract
import android.text.TextUtils
import expo.modules.calendar.availabilityConstantMatchingString
import expo.modules.calendar.next.exceptions.CalendarCouldNotBeUpdatedException
import expo.modules.calendar.next.exceptions.EventsCouldNotBeCreatedException
import expo.modules.calendar.next.records.AlarmMethod
import expo.modules.calendar.next.records.AttendeeType
import expo.modules.calendar.next.records.CalendarAccessLevel
import expo.modules.calendar.next.records.CalendarRecord
import expo.modules.calendar.next.records.EventRecord
import expo.modules.calendar.next.records.Source
import expo.modules.calendar.next.utils.findEvents
import expo.modules.calendar.next.utils.optIntFromCursor
import expo.modules.calendar.next.utils.optStringFromCursor
import expo.modules.kotlin.AppContext
import expo.modules.kotlin.apifeatures.EitherType
import expo.modules.kotlin.exception.Exceptions
import expo.modules.kotlin.sharedobjects.SharedObject
import java.util.TimeZone

@OptIn(EitherType::class)
class ExpoCalendar : SharedObject {
  val localAppContext: AppContext
  var calendarRecord: CalendarRecord?

  constructor(appContext: AppContext, calendar: CalendarRecord) {
    this.localAppContext = appContext
    this.calendarRecord = calendar
  }

  constructor(appContext: AppContext, cursor: Cursor) {
    this.localAppContext = appContext
    this.calendarRecord = CalendarRecord(
      id = optStringFromCursor(cursor, CalendarContract.Calendars._ID),
      title = optStringFromCursor(cursor, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME),
      isPrimary = optIntFromCursor(cursor, CalendarContract.Calendars.IS_PRIMARY) == 1,
      name = optStringFromCursor(cursor, CalendarContract.Calendars.NAME),
      color = optIntFromCursor(cursor, CalendarContract.Calendars.CALENDAR_COLOR),
      ownerAccount = optStringFromCursor(cursor, CalendarContract.Calendars.OWNER_ACCOUNT),
      timeZone = optStringFromCursor(cursor, CalendarContract.Calendars.CALENDAR_TIME_ZONE),
      isVisible = optIntFromCursor(cursor, CalendarContract.Calendars.VISIBLE) != 0,
      isSynced = optIntFromCursor(cursor, CalendarContract.Calendars.SYNC_EVENTS) != 0,
      allowsModifications = optIntFromCursor(cursor, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL) == CalendarContract.Calendars.CAL_ACCESS_ROOT ||
        optIntFromCursor(cursor, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL) == CalendarContract.Calendars.CAL_ACCESS_OWNER ||
        optIntFromCursor(cursor, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL) == CalendarContract.Calendars.CAL_ACCESS_EDITOR ||
        optIntFromCursor(cursor, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL) == CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR,
      accessLevel = optStringFromCursor(cursor, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)?.let { accessLevelString ->
        try {
          CalendarAccessLevel.entries.find { it.value == accessLevelString }
            ?: CalendarAccessLevel.NONE
        } catch (_: Exception) {
          CalendarAccessLevel.NONE
        }
      } ?: CalendarAccessLevel.NONE,
      allowedReminders = optStringFromCursor(cursor, CalendarContract.Calendars.ALLOWED_REMINDERS)?.split(",")?.filter { it.isNotEmpty() }?.map { reminderString ->
        try {
          AlarmMethod.entries.find { it.value == reminderString } ?: AlarmMethod.DEFAULT
        } catch (_: Exception) {
          AlarmMethod.DEFAULT
        }
      } ?: emptyList(),
      allowedAttendeeTypes = optStringFromCursor(cursor, CalendarContract.Calendars.ALLOWED_ATTENDEE_TYPES)?.split(",")?.filter { it.isNotEmpty() }?.map { attendeeTypeString ->
        try {
          AttendeeType.entries.find { it.value == attendeeTypeString } ?: AttendeeType.NONE
        } catch (_: Exception) {
          AttendeeType.NONE
        }
      } ?: emptyList(),
      source = Source(
        id = optStringFromCursor(cursor, CalendarContract.Calendars.ACCOUNT_NAME),
        type = optStringFromCursor(cursor, CalendarContract.Calendars.ACCOUNT_TYPE),
        name = optStringFromCursor(cursor, CalendarContract.Calendars.ACCOUNT_NAME),
        isLocalAccount = optStringFromCursor(cursor, CalendarContract.Calendars.ACCOUNT_TYPE) == CalendarContract.ACCOUNT_TYPE_LOCAL
      ),
    )
  }

  suspend fun getEvents(startDate: Any, endDate: Any): List<ExpoCalendarEvent> {
    if (calendarRecord?.id == null) {
      throw Exception("Calendar id is null")
    }
    val contentResolver = (appContext?.reactContext
      ?: throw Exceptions.ReactContextLost()).contentResolver
    val cursor = findEvents(contentResolver, startDate, endDate, listOf(calendarRecord?.id
      ?: ""))
    return cursor.use { serializeExpoCalendarEvents(cursor) }
  }

  fun deleteCalendar(): Boolean {
    val rows: Int
    val calendarID = calendarRecord?.id?.toIntOrNull()
    if (calendarID == null) {
      throw Exceptions.IllegalStateException("E_CALENDAR_NOT_DELETED")
    }
    val uri = ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, calendarID.toLong())
    val contentResolver = (appContext?.reactContext
      ?: throw Exceptions.ReactContextLost()).contentResolver
    rows = contentResolver.delete(uri, null, null)
    calendarRecord = null
    return rows > 0
  }

  fun createEvent(record: EventRecord): ExpoCalendarEvent? {
    val event = ExpoCalendarEvent(localAppContext, record)
    val calendarId = this.calendarRecord?.id
    if (calendarId == null) {
      throw EventsCouldNotBeCreatedException("Calendar id is null")
    }
    val newEventId = event.saveEvent(record, calendarId)
    event.reloadEvent(newEventId.toString())
    return event
  }

  private fun serializeExpoCalendarEvents(cursor: Cursor): List<ExpoCalendarEvent> {
    val results: MutableList<ExpoCalendarEvent> = ArrayList()
    while (cursor.moveToNext()) {
      results.add(ExpoCalendarEvent(localAppContext, cursor))
    }
    return results
  }

  companion object {
    fun saveCalendar(calendarRecord: CalendarRecord, appContext: AppContext): Int {
      return updateCalendar(calendarRecord, appContext, isNew = true)
    }

    fun updateCalendar(calendarRecord: CalendarRecord, appContext: AppContext, isNew: Boolean = false): Int {
      if (isNew) {
        if (calendarRecord.title == null) {
          throw CalendarCouldNotBeUpdatedException("new calendars require `title`")
        }
        if (calendarRecord.name == null) {
          throw CalendarCouldNotBeUpdatedException("new calendars require `name`")
        }
        if (calendarRecord.source == null) {
          throw CalendarCouldNotBeUpdatedException("new calendars require `source`")
        }
        if (calendarRecord.color == null) {
          throw CalendarCouldNotBeUpdatedException("new calendars require `color`")
        }
      }

      val source = calendarRecord.source
      if (isNew && (source?.name == null)) {
        throw CalendarCouldNotBeUpdatedException("new calendars require a `source` object with a `name`")
      }

      val values = ContentValues().apply {
        calendarRecord.name?.let { put(CalendarContract.Calendars.NAME, it) }
        calendarRecord.title?.let { put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, it) }
        put(CalendarContract.Calendars.VISIBLE, calendarRecord.isVisible)
        put(CalendarContract.Calendars.SYNC_EVENTS, calendarRecord.isSynced)

        if (isNew) {
          source?.name?.let { put(CalendarContract.Calendars.ACCOUNT_NAME, it) }
          source?.let { put(CalendarContract.Calendars.ACCOUNT_TYPE, if (it.isLocalAccount) CalendarContract.ACCOUNT_TYPE_LOCAL else it.type) }
        }

        calendarRecord.color?.let { put(CalendarContract.Calendars.CALENDAR_COLOR, it) }

        if (isNew) {
          calendarRecord.accessLevel?.let { accessLevel ->
            val accessLevelValue = when (accessLevel) {
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
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, accessLevelValue)
          }
        }
        if (isNew) {
          calendarRecord.ownerAccount?.let { put(CalendarContract.Calendars.OWNER_ACCOUNT, it) }
        }

        if (isNew) {
          calendarRecord.timeZone?.let { put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, it) }
            ?: put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().id)
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

      val contentResolver = (appContext.reactContext
        ?: throw Exceptions.ReactContextLost()).contentResolver

      return if (isNew) {
        val uriBuilder = CalendarContract.Calendars.CONTENT_URI
          .buildUpon()
          .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
          .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, source!!.name)
          .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, if (source.isLocalAccount) CalendarContract.ACCOUNT_TYPE_LOCAL else source.type)

        val calendarsUri = uriBuilder.build()
        val calendarUri = contentResolver.insert(calendarsUri, values)
        val calendarId = calendarUri?.lastPathSegment!!.toInt()
        calendarId
      } else {
        val uri = CalendarContract.Calendars.CONTENT_URI.buildUpon().appendPath(calendarRecord.id).build()
        val rowsUpdated = contentResolver.update(uri, values, null, null)
        if (rowsUpdated == 0) {
          throw CalendarCouldNotBeUpdatedException("Failed to update calendar")
        }
        calendarRecord.id!!.toInt()
      }
    }
  }
}
