package expo.modules.calendar.next.records

import android.provider.CalendarContract
import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record

data class AttendeeRecord(
  @Field
  var id: String? = null,
  @Field
  val name: String? = null,
  @Field
  val role: AttendeeRole? = null,
  @Field
  val status: AttendeeStatus? = null,
  @Field
  val type: AttendeeType? = null,
  @Field
  val email: String? = null,
) : Record {
  fun getUpdatedRecord(other: AttendeeRecord, nullableFields: List<String>? = null): AttendeeRecord {
    val nullableSet = nullableFields?.toSet() ?: emptySet()

    return AttendeeRecord(
      id = this.id,
      name = if ("name" in nullableSet) null else other.name ?: this.name,
      role = if ("role" in nullableSet) null else other.role ?: this.role,
      status = if ("status" in nullableSet) null else other.status ?: this.status,
      type = if ("type" in nullableSet) null else other.type ?: this.type,
      email = if ("email" in nullableSet) null else other.email ?: this.email,
    )
  }
}

enum class AttendeeRole(val value: String, val androidValue: Int) {
  ATTENDEE("attendee", CalendarContract.Attendees.RELATIONSHIP_ATTENDEE),
  ORGANIZER("organizer", CalendarContract.Attendees.RELATIONSHIP_ORGANIZER),
  PERFORMER("performer", CalendarContract.Attendees.RELATIONSHIP_PERFORMER),
  SPEAKER("speaker", CalendarContract.Attendees.RELATIONSHIP_SPEAKER),
  NONE("none", CalendarContract.Attendees.RELATIONSHIP_NONE);

  companion object {
    fun fromAndroidValue(value: Int): AttendeeRole? = entries.find { it.androidValue == value }
  }
}

enum class AttendeeStatus(val value: String, val androidValue: Int) {
    ACCEPTED("accepted", CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED),
    DECLINED("declined", CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED),
    INVITED("invited", CalendarContract.Attendees.ATTENDEE_STATUS_INVITED),
    TENTATIVE("tentative", CalendarContract.Attendees.ATTENDEE_STATUS_TENTATIVE),
    NONE("none", CalendarContract.Attendees.ATTENDEE_STATUS_NONE);

  companion object {
    fun fromAndroidValue(value: Int): AttendeeStatus? = entries.find { it.androidValue == value }
  }
}

enum class AttendeeType(val value: String, val androidValue: Int) {
  RESOURCE("resource", CalendarContract.Attendees.TYPE_RESOURCE),
  OPTIONAL("optional", CalendarContract.Attendees.TYPE_OPTIONAL),
  REQUIRED("required", CalendarContract.Attendees.TYPE_REQUIRED),
  NONE("none", CalendarContract.Attendees.TYPE_NONE);

  companion object {
    fun fromAndroidValue(value: Int): AttendeeType? = entries.find { it.androidValue == value }
  }
}
