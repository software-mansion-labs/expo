package expo.modules.calendar.next

import android.content.ContentResolver
import android.database.Cursor
import android.provider.CalendarContract
import expo.modules.calendar.CalendarUtils
import expo.modules.calendar.attendeeRelationshipStringMatchingConstant
import expo.modules.calendar.attendeeStatusStringMatchingConstant
import expo.modules.calendar.attendeeTypeStringMatchingConstant
import expo.modules.calendar.next.records.AttendeeRecord
import expo.modules.kotlin.sharedobjects.SharedObject

class ExpoCalendarAttendee : SharedObject {
  var attendeeRecord: AttendeeRecord?
  var contentResolver: ContentResolver

  constructor(contentResolver: ContentResolver) {
    this.contentResolver = contentResolver
    this.attendeeRecord = null;
  }

  constructor(contentResolver: ContentResolver, cursor: Cursor) {
    this.contentResolver = contentResolver
    this.attendeeRecord = AttendeeRecord(
      id = CalendarUtils.optStringFromCursor(cursor, CalendarContract.Attendees._ID),
      name = CalendarUtils.optStringFromCursor(cursor, CalendarContract.Attendees.ATTENDEE_NAME),
      role = attendeeRelationshipStringMatchingConstant(CalendarUtils.optIntFromCursor(cursor, CalendarContract.Attendees.ATTENDEE_RELATIONSHIP)),
      status = attendeeStatusStringMatchingConstant(CalendarUtils.optIntFromCursor(cursor, CalendarContract.Attendees.ATTENDEE_STATUS)),
      type = attendeeTypeStringMatchingConstant(CalendarUtils.optIntFromCursor(cursor, CalendarContract.Attendees.ATTENDEE_TYPE)),
      email = CalendarUtils.optStringFromCursor(cursor, CalendarContract.Attendees.ATTENDEE_EMAIL),
    )
  }
}
