package expo.modules.calendar.next.records

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
  val availability: String? = null,
  @Field
  val status: String? = null,
  @Field
  val organizerEmail: String? = null,
  @Field
  val accessLevel: String? = null,
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

data class AttendeeRecord(
  @Field
  val id: String? = null,
  @Field
  val name: String? = null,
  @Field
  val role: String? = null,
  @Field
  val status: String? = null,
  @Field
  val type: String? = null,
  @Field
  val email: String? = null,
) : Record
