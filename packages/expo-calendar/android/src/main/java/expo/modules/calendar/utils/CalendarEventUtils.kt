package expo.modules.calendar.utils

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import expo.modules.calendar.CalendarEventBuilder
import expo.modules.calendar.CalendarModule.Companion.TAG
import expo.modules.calendar.CalendarUtils
import expo.modules.calendar.EventNotSavedException
import expo.modules.calendar.EventRecurrenceUtils.createRecurrenceRule
import expo.modules.calendar.EventRecurrenceUtils.extractRecurrence
import expo.modules.calendar.accessConstantMatchingString
import expo.modules.calendar.availabilityConstantMatchingString
import expo.modules.calendar.calAccessStringMatchingConstant
import expo.modules.calendar.calendarAllowedAttendeeTypesFromDBString
import expo.modules.calendar.calendarAllowedAvailabilitiesFromDBString
import expo.modules.calendar.calendarAllowedRemindersFromDBString
import expo.modules.calendar.findCalendarByIdQueryFields
import expo.modules.calendar.reminderConstantMatchingString
import expo.modules.core.arguments.ReadableArguments
import expo.modules.core.errors.InvalidArgumentException
import java.text.ParseException
import java.util.*

object CalendarEventUtils {
  @Throws(EventNotSavedException::class, ParseException::class, SecurityException::class, InvalidArgumentException::class)
  fun saveEvent(
    contentResolver: ContentResolver,
    details: ReadableArguments
  ): Int {
    val calendarEventBuilder = CalendarEventBuilder(details)
    if (details.containsKey("startDate")) {
      val startCal = Calendar.getInstance()
      val startDate = details["startDate"]
      try {
        when (startDate) {
          is String -> {
            val parsedDate = CalendarUtils.sdf.parse(startDate)
            if (parsedDate != null) {
              startCal.time = parsedDate
              calendarEventBuilder.put(CalendarContract.Events.DTSTART, startCal.timeInMillis)
            } else {
              Log.e(TAG, "Parsed date is null")
            }
          }

          is Number -> {
            calendarEventBuilder.put(CalendarContract.Events.DTSTART, startDate.toLong())
          }

          else -> {
            Log.e(TAG, "startDate has unsupported type")
          }
        }
      } catch (e: ParseException) {
        Log.e(TAG, "error", e)
        throw e
      }
    }
    if (details.containsKey("endDate")) {
      val endCal = Calendar.getInstance()
      val endDate = details["endDate"]
      try {
        if (endDate is String) {
          val parsedDate = CalendarUtils.sdf.parse(endDate)
          if (parsedDate != null) {
            endCal.time = parsedDate
            calendarEventBuilder.put(CalendarContract.Events.DTEND, endCal.timeInMillis)
          } else {
            Log.e(TAG, "Parsed date is null")
          }
        } else if (endDate is Number) {
          calendarEventBuilder.put(CalendarContract.Events.DTEND, endDate.toLong())
        }
      } catch (e: ParseException) {
        Log.e(TAG, "error", e)
        throw e
      }
    }
    if (details.containsKey("recurrenceRule")) {
      val recurrenceRule = details.getArguments("recurrenceRule")
      if (recurrenceRule.containsKey("frequency")) {
        val opts = extractRecurrence(recurrenceRule)

        if (opts.endDate == null && opts.occurrence == null) {
          val eventStartDate = calendarEventBuilder.getAsLong(CalendarContract.Events.DTSTART)
          val eventEndDate = calendarEventBuilder.getAsLong(CalendarContract.Events.DTEND)
          val duration = (eventEndDate - eventStartDate) / 1000
          calendarEventBuilder
            .putNull(CalendarContract.Events.LAST_DATE)
            .putNull(CalendarContract.Events.DTEND)
            .put(CalendarContract.Events.DURATION, "PT${duration}S")
        }
        val rule = createRecurrenceRule(opts)
        calendarEventBuilder.put(CalendarContract.Events.RRULE, rule)
      }
    }

    calendarEventBuilder
      .putEventBoolean(CalendarContract.Events.HAS_ALARM, "alarms", true)
      .putEventString(CalendarContract.Events.AVAILABILITY, "availability", ::availabilityConstantMatchingString)
      .putEventString(CalendarContract.Events.TITLE, "title")
      .putEventString(CalendarContract.Events.DESCRIPTION, "notes")
      .putEventString(CalendarContract.Events.EVENT_LOCATION, "location")
      .putEventString(CalendarContract.Events.ORGANIZER, "organizerEmail")
      .putEventBoolean(CalendarContract.Events.ALL_DAY, "allDay")
      .putEventBoolean(CalendarContract.Events.GUESTS_CAN_MODIFY, "guestsCanModify")
      .putEventBoolean(CalendarContract.Events.GUESTS_CAN_INVITE_OTHERS, "guestsCanInviteOthers")
      .putEventBoolean(CalendarContract.Events.GUESTS_CAN_SEE_GUESTS, "guestsCanSeeGuests")
      .putEventTimeZone(CalendarContract.Events.EVENT_TIMEZONE, "timeZone")
      .putEventTimeZone(CalendarContract.Events.EVENT_END_TIMEZONE, "endTimeZone")
      .putEventString(CalendarContract.Events.ACCESS_LEVEL, "accessLevel", ::accessConstantMatchingString)

    return if (details.containsKey("id")) {
      val eventID = details.getString("id").toInt()
      val updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventID.toLong())
      contentResolver.update(updateUri, calendarEventBuilder.build(), null, null)
      removeRemindersForEvent(contentResolver, eventID)
      if (details.containsKey("alarms")) {
        createRemindersForEvent(contentResolver, eventID, details.getList("alarms"))
      }
      eventID
    } else {
      if (details.containsKey("calendarId")) {
        val calendar = findCalendarById(contentResolver, details.getString("calendarId"))
        if (calendar != null) {
          calendarEventBuilder.put(CalendarContract.Events.CALENDAR_ID, calendar.getString("id")!!.toInt())
        } else {
          throw InvalidArgumentException("Couldn't find calendar with given id: " + details.getString("calendarId"))
        }
      } else {
        throw InvalidArgumentException("CalendarId is required.")
      }
      val eventsUri = CalendarContract.Events.CONTENT_URI
      val eventUri = contentResolver.insert(eventsUri, calendarEventBuilder.build())
        ?: throw EventNotSavedException()
      val eventID = eventUri.lastPathSegment!!.toInt()
      if (details.containsKey("alarms")) {
        createRemindersForEvent(contentResolver, eventID, details.getList("alarms"))
      }
      eventID
    }
  }

  fun findCalendarById(contentResolver: ContentResolver, calendarID: String): Bundle? {
    val uri = ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, calendarID.toInt().toLong())
    val cursor = contentResolver.query(
      uri,
      findCalendarByIdQueryFields,
      null,
      null,
      null
    )
    requireNotNull(cursor) { "Cursor shouldn't be null" }
    return cursor.use {
      if (it.count > 0) {
        it.moveToFirst()
        serializeEventCalendar(it)
      } else {
        null
      }
    }
  }

  @Throws(SecurityException::class)
  fun createRemindersForEvent(contentResolver: ContentResolver, eventID: Int, reminders: List<*>) {
    for (i in reminders.indices) {
      val reminder = reminders[i] as Map<*, *>
      val relativeOffset = reminder["relativeOffset"]
      if (relativeOffset is Number) {
        val minutes = -relativeOffset.toInt()
        var method = CalendarContract.Reminders.METHOD_DEFAULT
        val reminderValues = ContentValues()
        if (reminder.containsKey("method")) {
          method = reminderConstantMatchingString(reminder["method"] as? String)
        }
        reminderValues.put(CalendarContract.Reminders.EVENT_ID, eventID)
        reminderValues.put(CalendarContract.Reminders.MINUTES, minutes)
        reminderValues.put(CalendarContract.Reminders.METHOD, method)
        contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
      }
    }
  }

  @Throws(SecurityException::class)
  fun removeRemindersForEvent(contentResolver: ContentResolver, eventID: Int) {
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

  @Throws(ParseException::class, SecurityException::class)
  fun removeEvent(contentResolver: ContentResolver, details: ReadableArguments): Boolean {
    val rows: Int
    val eventID = details.getString("id").toInt()
    if (!details.containsKey("instanceStartDate")) {
      val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventID.toLong())
      rows = contentResolver.delete(uri, null, null)
      return rows > 0
    } else {
      val exceptionValues = ContentValues()
      val startCal = Calendar.getInstance()
      val instanceStartDate = details["instanceStartDate"]
      try {
        if (instanceStartDate is String) {
          val parsedDate = CalendarUtils.sdf.parse(instanceStartDate)
          if (parsedDate != null) {
            startCal.time = parsedDate
            exceptionValues.put(CalendarContract.Events.ORIGINAL_INSTANCE_TIME, startCal.timeInMillis)
          } else {
            Log.e(TAG, "Parsed date is null")
          }
        } else if (instanceStartDate is Number) {
          exceptionValues.put(CalendarContract.Events.ORIGINAL_INSTANCE_TIME, instanceStartDate.toLong())
        }
      } catch (e: ParseException) {
        Log.e(TAG, "error", e)
        throw e
      }
      exceptionValues.put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CANCELED)
      val exceptionUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_EXCEPTION_URI, eventID.toLong())
      contentResolver.insert(exceptionUri, exceptionValues)
    }
    return true
  }

  private fun serializeEventCalendar(cursor: Cursor): Bundle {
    val calendar = Bundle().apply {
      putString("id", CalendarUtils.optStringFromCursor(cursor, android.provider.CalendarContract.Calendars._ID))
      putString("title", CalendarUtils.optStringFromCursor(cursor, android.provider.CalendarContract.Calendars.CALENDAR_DISPLAY_NAME))
      putBoolean("isPrimary", CalendarUtils.optIntFromCursor(cursor, android.provider.CalendarContract.Calendars.IS_PRIMARY) == 1)
      putStringArrayList("allowedAvailabilities", calendarAllowedAvailabilitiesFromDBString(CalendarUtils.stringFromCursor(cursor, android.provider.CalendarContract.Calendars.ALLOWED_AVAILABILITY)))
      putString("name", CalendarUtils.optStringFromCursor(cursor, android.provider.CalendarContract.Calendars.NAME))
      putString("color", String.format("#%06X", 0xFFFFFF and CalendarUtils.optIntFromCursor(cursor, android.provider.CalendarContract.Calendars.CALENDAR_COLOR)))
      putString("ownerAccount", CalendarUtils.optStringFromCursor(cursor, android.provider.CalendarContract.Calendars.OWNER_ACCOUNT))
      putString("timeZone", CalendarUtils.optStringFromCursor(cursor, android.provider.CalendarContract.Calendars.CALENDAR_TIME_ZONE))
      putStringArrayList("allowedReminders", calendarAllowedRemindersFromDBString(CalendarUtils.stringFromCursor(cursor, android.provider.CalendarContract.Calendars.ALLOWED_REMINDERS)))
      putStringArrayList("allowedAttendeeTypes", calendarAllowedAttendeeTypesFromDBString(CalendarUtils.stringFromCursor(cursor, android.provider.CalendarContract.Calendars.ALLOWED_ATTENDEE_TYPES)))
      putBoolean("isVisible", CalendarUtils.optIntFromCursor(cursor, android.provider.CalendarContract.Calendars.VISIBLE) != 0)
      putBoolean("isSynced", CalendarUtils.optIntFromCursor(cursor, android.provider.CalendarContract.Calendars.SYNC_EVENTS) != 0)
      val accessLevel = CalendarUtils.optIntFromCursor(cursor, android.provider.CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)
      putString("accessLevel", calAccessStringMatchingConstant(accessLevel))
      putBoolean(
        "allowsModifications",
        accessLevel == android.provider.CalendarContract.Calendars.CAL_ACCESS_ROOT ||
          accessLevel == android.provider.CalendarContract.Calendars.CAL_ACCESS_OWNER ||
          accessLevel == android.provider.CalendarContract.Calendars.CAL_ACCESS_EDITOR ||
          accessLevel == android.provider.CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR
      )
    }
    val source = Bundle().apply {
      putString("name", CalendarUtils.optStringFromCursor(cursor, android.provider.CalendarContract.Calendars.ACCOUNT_NAME))
      val type = CalendarUtils.optStringFromCursor(cursor, android.provider.CalendarContract.Calendars.ACCOUNT_TYPE)
      putString("type", type)
      putBoolean("isLocalAccount", type == android.provider.CalendarContract.ACCOUNT_TYPE_LOCAL)
    }
    calendar.putBundle("source", source)
    return calendar
  }
}
