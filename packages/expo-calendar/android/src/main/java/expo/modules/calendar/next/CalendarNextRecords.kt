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
  val startDate: String? = null,
  @Field
  val endDate: String? = null,
  @Field
  val allDay: Boolean? = null,
  @Field
  val availability: String? = null,
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
      startDate = if ("startDate" in nullableSet) null else other.startDate ?: this.startDate,
      endDate = if ("endDate" in nullableSet) null else other.endDate ?: this.endDate,
      allDay = if ("allDay" in nullableSet) null else other.allDay ?: this.allDay,
      availability = if ("availability" in nullableSet) null else other.availability ?: this.availability
    )
  }
}

data class AlarmRecord (
  @Field
  val relativeOffset: Int,
  @Field
  val method: Int,
) : Record

data class RecurringEventOptions(
  @Field
  val instanceStartDate: String? = null,
) : Record
