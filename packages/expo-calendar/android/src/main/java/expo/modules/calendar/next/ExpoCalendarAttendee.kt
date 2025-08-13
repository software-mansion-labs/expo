package expo.modules.calendar.next

import android.content.ContentResolver
import android.database.Cursor
import android.provider.CalendarContract
import expo.modules.calendar.CalendarUtils
import expo.modules.calendar.attendeeRelationshipStringMatchingConstant
import expo.modules.calendar.attendeeStatusStringMatchingConstant
import expo.modules.calendar.attendeeTypeStringMatchingConstant
import expo.modules.calendar.next.records.AttendeeRecord
import expo.modules.kotlin.AppContext
import expo.modules.kotlin.sharedobjects.SharedObject

class ExpoCalendarAttendee : SharedObject {
  var attendeeRecord: AttendeeRecord?
  var localAppContext: AppContext

  constructor(appContext: AppContext) {
    this.localAppContext = appContext
    this.attendeeRecord = null;
  }

  constructor(appContext: AppContext, cursor: Cursor) {
    this.localAppContext = appContext
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
