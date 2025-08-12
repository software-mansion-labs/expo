package expo.modules.calendar.next

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.provider.CalendarContract
import expo.modules.calendar.CalendarUtils
import expo.modules.calendar.EventNotSavedException
import expo.modules.calendar.next.records.EventRecord
import expo.modules.core.errors.InvalidArgumentException
import expo.modules.kotlin.apifeatures.EitherType
import expo.modules.kotlin.sharedobjects.SharedObject
import java.text.ParseException
import java.util.*


@OptIn(EitherType::class)
class ExpoCalendarEvent : SharedObject {
  var eventRecord: EventRecord?

  val sdf = CalendarUtils.sdf
  private val contentResolver: ContentResolver;

  constructor(contentResolver: ContentResolver) {
    this.contentResolver = contentResolver;
    this.eventRecord = null
  }

  constructor(contentResolver: ContentResolver, eventRecord: EventRecord) {
    this.contentResolver = contentResolver
    this.eventRecord = eventRecord
  }

  constructor(contentResolver: ContentResolver, cursor: Cursor) {
    this.contentResolver = contentResolver;
    var foundStartDate: String? = null;
    var foundEndDate: String? = null;

    // may be CalendarContract.Instances.BEGIN or CalendarContract.Events.DTSTART (which have different string values)
    val startDate = cursor.getString(3)
    if (startDate != null) {
      foundStartDate = CalendarUtils.sdf.format(Date(startDate.toLong()));
    }
    val endDate = cursor.getString(4)
    if (endDate != null) {
      foundEndDate = CalendarUtils.sdf.format(Date(endDate.toLong()));
    }
    
    this.eventRecord = EventRecord(
      id = CalendarUtils.optStringFromCursor(cursor, CalendarContract.Instances.EVENT_ID),
      calendarId = CalendarUtils.optStringFromCursor(cursor, CalendarContract.Events.CALENDAR_ID),
      title = CalendarUtils.optStringFromCursor(cursor, CalendarContract.Events.TITLE),
      notes = CalendarUtils.optStringFromCursor(cursor, CalendarContract.Events.DESCRIPTION),
      startDate = foundStartDate,
      endDate = foundEndDate,
      allDay = CalendarUtils.optIntFromCursor(cursor, CalendarContract.Events.ALL_DAY) != 0,
      location = CalendarUtils.optStringFromCursor(cursor, CalendarContract.Events.EVENT_LOCATION),
      availability = CalendarUtils.optStringFromCursor(cursor, CalendarContract.Events.AVAILABILITY),
      timeZone = CalendarUtils.optStringFromCursor(cursor, CalendarContract.Events.EVENT_TIMEZONE),
      endTimeZone = CalendarUtils.optStringFromCursor(cursor, CalendarContract.Events.EVENT_END_TIMEZONE),
    )
  }

  @Throws(EventNotSavedException::class, ParseException::class, SecurityException::class, InvalidArgumentException::class)
  fun saveEvent(eventRecord: EventRecord, calendarId: String? = null): Int? {
    val eventBuilder = CalendarEventBuilderNext()

    if (eventRecord.startDate != null) {
      val parsedDate = sdf.parse(eventRecord.startDate)
      if (parsedDate != null) {
        val startCal = Calendar.getInstance()
        startCal.time = parsedDate
        eventBuilder.put(CalendarContract.Events.DTSTART, startCal.timeInMillis)
      }
    }

    if (eventRecord.endDate != null) {
      val parsedDate = sdf.parse(eventRecord.endDate)
      if (parsedDate != null) {
        val endCal = Calendar.getInstance()
        endCal.time = parsedDate
        eventBuilder.put(CalendarContract.Events.DTEND, endCal.timeInMillis)
      }
    }

    if (eventRecord.title != null) {
      eventBuilder.put(CalendarContract.Events.TITLE, eventRecord.title)
    }
    if (eventRecord.notes != null) {
      eventBuilder.put(CalendarContract.Events.DESCRIPTION, eventRecord.notes)
    }

    if (eventRecord.timeZone != null) {
      eventBuilder.put(CalendarContract.Events.EVENT_TIMEZONE, eventRecord.timeZone)
    } else {
      eventBuilder.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
    }

    if (this.eventRecord?.id != null) {
      // Update current event
      val eventID = eventRecord.id?.toInt()
      if (eventID == null) {
        throw InvalidArgumentException("Event ID is required")
      }
      val updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventID.toLong())
      contentResolver.update(updateUri, eventBuilder.build(), null, null)
      return eventID
    } else {
      if (calendarId == null) {
        throw InvalidArgumentException("CalendarId is required.")
      }
      eventBuilder.put(CalendarContract.Events.CALENDAR_ID, calendarId.toInt())
      val eventsUri = CalendarContract.Events.CONTENT_URI
      val eventUri = contentResolver.insert(eventsUri, eventBuilder.build())
        ?: throw EventNotSavedException()
      val eventID = eventUri.lastPathSegment!!.toInt()
      return eventID
    }
  }
}
