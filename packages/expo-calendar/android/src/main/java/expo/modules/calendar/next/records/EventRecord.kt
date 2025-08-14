package expo.modules.calendar.next.records

import android.provider.CalendarContract
import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record

data class EventRecord (
  @Field
  val id: String? = null,
  @Field
  val calendarId: String? = null,
  @Field
  val title: String? = null,
  @Field
  val location: String? = null,
  @Field
  val timeZone: String? = null,
  @Field
  val endTimeZone: String? = null,
  @Field
  val notes: String? = null,
  @Field
  val recurrenceRule: RecurrenceRuleRecord? = null,
  @Field
  val startDate: String? = null,
  @Field
  val endDate: String? = null,
  @Field
  val allDay: Boolean? = null,
  @Field
  val availability: EventAvailability? = null,
  @Field
  val status: EventStatus? = null,
  @Field
  val organizerEmail: String? = null,
  @Field
  val accessLevel: EventAccessLevel? = null,
  @Field
  val guestsCanModify: Boolean? = null,
  @Field
  val guestsCanInviteOthers: Boolean? = null,
  @Field
  val guestsCanSeeGuests: Boolean? = null,
  @Field
  val originalId: String? = null,
  @Field
  val instanceId: String? = null,
) : Record {
  fun getUpdatedRecord(other: EventRecord, nullableFields: List<String>? = null): EventRecord {
    val nullableSet = nullableFields?.toSet() ?: emptySet()

    return EventRecord(
      id = if ("id" in nullableSet) null else other.id ?: this.id,
      calendarId = if ("calendarId" in nullableSet) null else other.calendarId ?: this.calendarId,
      title = if ("title" in nullableSet) null else other.title ?: this.title,
      location = if ("location" in nullableSet) null else other.location ?: this.location,
      timeZone = if ("timeZone" in nullableSet) null else other.timeZone ?: this.timeZone,
      endTimeZone = if ("endTimeZone" in nullableSet) null else other.endTimeZone ?: this.endTimeZone,
      notes = if ("notes" in nullableSet) null else other.notes ?: this.notes,
      recurrenceRule = if ("recurrenceRule" in nullableSet) null else other.recurrenceRule ?: this.recurrenceRule,
      startDate = if ("startDate" in nullableSet) null else other.startDate ?: this.startDate,
      endDate = if ("endDate" in nullableSet) null else other.endDate ?: this.endDate,
      allDay = if ("allDay" in nullableSet) null else other.allDay ?: this.allDay,
      availability = if ("availability" in nullableSet) null else other.availability ?: this.availability,
      status = if ("status" in nullableSet) null else other.status ?: this.status,
      organizerEmail = if ("organizerEmail" in nullableSet) null else other.organizerEmail ?: this.organizerEmail,
      accessLevel = if ("accessLevel" in nullableSet) null else other.accessLevel ?: this.accessLevel,
      guestsCanModify = if ("guestsCanModify" in nullableSet) null else other.guestsCanModify ?: this.guestsCanModify,
      guestsCanInviteOthers = if ("guestsCanInviteOthers" in nullableSet) null else other.guestsCanInviteOthers ?: this.guestsCanInviteOthers,
      guestsCanSeeGuests = if ("guestsCanSeeGuests" in nullableSet) null else other.guestsCanSeeGuests ?: this.guestsCanSeeGuests,
      originalId = if ("originalId" in nullableSet) null else other.originalId ?: this.originalId,
    )
  }
}

data class AlarmRecord (
  @Field
  val relativeOffset: Int,
  @Field
  val method: Int,
) : Record

data class RecurrenceRuleRecord(
  @Field
  val endDate: String? = null,
  @Field
  val frequency: String? = null,
  @Field
  val interval: Int? = null,
  @Field
  val occurrence: Int? = null,
) : Record

data class RecurringEventOptions(
  @Field
  val instanceStartDate: String? = null,
) : Record

enum class EventAvailability(val value: String, val androidValue: Int) {
  BUSY("busy", CalendarContract.Events.AVAILABILITY_BUSY),
  FREE("free", CalendarContract.Events.AVAILABILITY_FREE),
  TENTATIVE("tentative", CalendarContract.Events.AVAILABILITY_TENTATIVE);

  companion object {
    fun fromAndroidValue(value: Int): EventAvailability? = entries.find { it.androidValue == value }
  }
}

enum class EventStatus(val value: String, val androidValue: Int) {
  CONFIRMED("confirmed", CalendarContract.Events.AVAILABILITY_BUSY),
  TENTATIVE("tentative", CalendarContract.Events.AVAILABILITY_FREE),
  CANCELED("canceled", CalendarContract.Events.AVAILABILITY_TENTATIVE);

  companion object {
    fun fromAndroidValue(value: Int): EventStatus? = entries.find { it.androidValue == value }
  }
}

enum class EventAccessLevel(val value: String, val androidValue: Int) {
  CONFIDENTIAL("confidential", CalendarContract.Events.ACCESS_CONFIDENTIAL),
  PRIVATE("private", CalendarContract.Events.ACCESS_PRIVATE),
  PUBLIC("public", CalendarContract.Events.ACCESS_PUBLIC),
  DEFAULT("default", CalendarContract.Events.ACCESS_DEFAULT);

  companion object {
    fun fromAndroidValue(value: Int): EventAccessLevel? = entries.find { it.androidValue == value }
  }
}
