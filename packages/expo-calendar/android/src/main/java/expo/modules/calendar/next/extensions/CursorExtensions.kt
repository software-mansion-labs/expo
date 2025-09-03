package expo.modules.calendar.next.extensions

import android.content.ContentResolver
import android.database.Cursor
import android.provider.CalendarContract
import android.util.Log
import expo.modules.calendar.CalendarModule.Companion.TAG
import expo.modules.calendar.next.records.AlarmMethod
import expo.modules.calendar.next.records.AlarmRecord
import expo.modules.calendar.next.records.AttendeeRecord
import expo.modules.calendar.next.records.AttendeeRole
import expo.modules.calendar.next.records.AttendeeStatus
import expo.modules.calendar.next.records.AttendeeType
import expo.modules.calendar.next.records.CalendarAccessLevel
import expo.modules.calendar.next.records.CalendarRecord
import expo.modules.calendar.next.records.EventAccessLevel
import expo.modules.calendar.next.records.EventAvailability
import expo.modules.calendar.next.records.EventRecord
import expo.modules.calendar.next.records.EventStatus
import expo.modules.calendar.next.records.RecurrenceRuleRecord
import expo.modules.calendar.next.records.Source
import expo.modules.calendar.next.utils.dateFormat
import expo.modules.calendar.next.utils.dateToString
import expo.modules.calendar.next.utils.optIntFromCursor
import expo.modules.calendar.next.utils.optStringFromCursor
import expo.modules.calendar.next.utils.rrFormat
import java.text.ParseException
import java.util.Locale

fun Cursor.toCalendarRecord() : CalendarRecord {
  return CalendarRecord(
    id = optStringFromCursor(this, CalendarContract.Calendars._ID),
    title = optStringFromCursor(this, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME),
    isPrimary = optIntFromCursor(this, CalendarContract.Calendars.IS_PRIMARY) == 1,
    name = optStringFromCursor(this, CalendarContract.Calendars.NAME),
    color = optIntFromCursor(this, CalendarContract.Calendars.CALENDAR_COLOR),
    ownerAccount = optStringFromCursor(this, CalendarContract.Calendars.OWNER_ACCOUNT),
    timeZone = optStringFromCursor(this, CalendarContract.Calendars.CALENDAR_TIME_ZONE),
    isVisible = optIntFromCursor(this, CalendarContract.Calendars.VISIBLE) != 0,
    isSynced = optIntFromCursor(this, CalendarContract.Calendars.SYNC_EVENTS) != 0,
    allowsModifications = optIntFromCursor(this, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL) == CalendarContract.Calendars.CAL_ACCESS_ROOT ||
      optIntFromCursor(this, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL) == CalendarContract.Calendars.CAL_ACCESS_OWNER ||
      optIntFromCursor(this, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL) == CalendarContract.Calendars.CAL_ACCESS_EDITOR ||
      optIntFromCursor(this, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL) == CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR,
    accessLevel = optStringFromCursor(this, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)?.let { accessLevelString ->
      try {
        CalendarAccessLevel.entries.find { it.value == accessLevelString }
          ?: CalendarAccessLevel.NONE
      } catch (_: Exception) {
        CalendarAccessLevel.NONE
      }
    } ?: CalendarAccessLevel.NONE,
    allowedReminders = optStringFromCursor(this, CalendarContract.Calendars.ALLOWED_REMINDERS)?.split(",")?.filter { it.isNotEmpty() }?.map { reminderString ->
      try {
        AlarmMethod.entries.find { it.value == reminderString } ?: AlarmMethod.DEFAULT
      } catch (_: Exception) {
        AlarmMethod.DEFAULT
      }
    } ?: emptyList(),
    allowedAttendeeTypes = optStringFromCursor(this, CalendarContract.Calendars.ALLOWED_ATTENDEE_TYPES)?.split(",")?.filter { it.isNotEmpty() }?.map { attendeeTypeString ->
      try {
        AttendeeType.entries.find { it.value == attendeeTypeString } ?: AttendeeType.NONE
      } catch (_: Exception) {
        AttendeeType.NONE
      }
    } ?: emptyList(),
    source = Source(
      id = optStringFromCursor(this, CalendarContract.Calendars.ACCOUNT_NAME),
      type = optStringFromCursor(this, CalendarContract.Calendars.ACCOUNT_TYPE),
      name = optStringFromCursor(this, CalendarContract.Calendars.ACCOUNT_NAME),
      isLocalAccount = optStringFromCursor(this, CalendarContract.Calendars.ACCOUNT_TYPE) == CalendarContract.ACCOUNT_TYPE_LOCAL
    ),
  )
}

fun Cursor.toAttendeeRecord(): AttendeeRecord {
  return AttendeeRecord(
    id = optStringFromCursor(this, CalendarContract.Attendees._ID),
    name = optStringFromCursor(this, CalendarContract.Attendees.ATTENDEE_NAME),
    role = AttendeeRole.fromAndroidValue(optIntFromCursor(this, CalendarContract.Attendees.ATTENDEE_RELATIONSHIP)),
    status = AttendeeStatus.fromAndroidValue(optIntFromCursor(this, CalendarContract.Attendees.ATTENDEE_STATUS)),
    type = AttendeeType.fromAndroidValue(optIntFromCursor(this, CalendarContract.Attendees.ATTENDEE_TYPE)),
    email = optStringFromCursor(this, CalendarContract.Attendees.ATTENDEE_EMAIL),
  )
}

fun Cursor.toEventRecord(contentResolver: ContentResolver): EventRecord {
  // may be CalendarContract.Instances.BEGIN or CalendarContract.Events.DTSTART (which have different string values)
  val startDate = this.getString(3)
  val endDate = this.getString(4)

  val eventId = optStringFromCursor(this, CalendarContract.Instances.EVENT_ID)
    ?: optStringFromCursor(this, CalendarContract.Instances._ID)

  // unfortunately the string values of CalendarContract.Events._ID and CalendarContract.Instances._ID are equal
  // so we'll use the somewhat brittle column number from the query
  val instanceId = if (this.columnCount > 18) optStringFromCursor(this, CalendarContract.Instances._ID) else "";

  return EventRecord(
    id = eventId,
    calendarId = optStringFromCursor(this, CalendarContract.Events.CALENDAR_ID),
    title = optStringFromCursor(this, CalendarContract.Events.TITLE),
    notes = optStringFromCursor(this, CalendarContract.Events.DESCRIPTION),
    alarms = if (eventId != null) serializeAlarms(contentResolver, eventId)?.toList() else null,
    recurrenceRule = extractRecurrenceRuleFromString(optStringFromCursor(this, CalendarContract.Events.RRULE)),
    startDate = dateToString(startDate?.toLongOrNull()),
    endDate = dateToString(endDate?.toLongOrNull()),
    allDay = optIntFromCursor(this, CalendarContract.Events.ALL_DAY) != 0,
    location = optStringFromCursor(this, CalendarContract.Events.EVENT_LOCATION),
    availability = EventAvailability.fromAndroidValue(optIntFromCursor(this, CalendarContract.Events.AVAILABILITY)),
    timeZone = optStringFromCursor(this, CalendarContract.Events.EVENT_TIMEZONE),
    endTimeZone = optStringFromCursor(this, CalendarContract.Events.EVENT_END_TIMEZONE),
    status = EventStatus.fromAndroidValue(optIntFromCursor(this, CalendarContract.Events.STATUS)),
    organizerEmail = optStringFromCursor(this, CalendarContract.Events.ORGANIZER),
    accessLevel = EventAccessLevel.fromAndroidValue(optIntFromCursor(this, CalendarContract.Events.ACCESS_LEVEL)),
    guestsCanModify = optIntFromCursor(this, CalendarContract.Events.GUESTS_CAN_MODIFY) != 0,
    guestsCanInviteOthers = optIntFromCursor(this, CalendarContract.Events.GUESTS_CAN_INVITE_OTHERS) != 0,
    guestsCanSeeGuests = optIntFromCursor(this, CalendarContract.Events.GUESTS_CAN_SEE_GUESTS) != 0,
    originalId = optStringFromCursor(this, CalendarContract.Events.ORIGINAL_ID),
    instanceId = instanceId
  )
}

private fun serializeAlarms(contentResolver: ContentResolver, eventId: String): ArrayList<AlarmRecord>? {
  val alarms = ArrayList<AlarmRecord>()
  val cursor = CalendarContract.Reminders.query(
    contentResolver,
    eventId.toLong(),
    arrayOf(
      CalendarContract.Reminders.MINUTES,
      CalendarContract.Reminders.METHOD
    )
  )
  while (cursor.moveToNext()) {
    val method = cursor.getInt(1)
    val thisAlarm = AlarmRecord(
      relativeOffset = -cursor.getInt(0),
      method = AlarmMethod.fromAndroidValue(method)
    )
    alarms.add(thisAlarm)
  }
  return alarms
}

private fun extractRecurrenceRuleFromString(rrule: String?): RecurrenceRuleRecord? {
  if (rrule == null) {
    return null
  }
  val ruleMap = mutableMapOf<String, String>()
  rrule.split(";").forEach { part ->
    val keyValue = part.split("=")
    if (keyValue.size == 2) {
      ruleMap[keyValue[0].uppercase(Locale.getDefault())] = keyValue[1]
    }
  }

  val frequency = ruleMap["FREQ"]?.lowercase(Locale.getDefault())
  val interval = ruleMap["INTERVAL"]?.toIntOrNull()
  var endDate: String? = null
  var occurrence: Int? = null

  ruleMap["UNTIL"]?.let { untilValue ->
    try {
      // Try to parse the UNTIL value using the known date format, fallback to raw string if parsing fails
      endDate = try {
        val date = rrFormat.parse(untilValue);
        if (date == null) {
          return null;
        }
        dateFormat.format(date)
      } catch (e: ParseException) {
        Log.e(TAG, "Couldn't parse the `endDate` property.", e)
        untilValue
      }
    } catch (e: Exception) {
      Log.e(TAG, "endDate is null or invalid", e)
      endDate = untilValue
    }
  }

  ruleMap["COUNT"]?.let { countValue ->
    occurrence = countValue.toIntOrNull()
  }
  return RecurrenceRuleRecord(
    endDate = endDate,
    frequency = frequency,
    interval = interval,
    occurrence = occurrence,
  )
}