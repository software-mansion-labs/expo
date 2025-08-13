package expo.modules.calendar.next.records

import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record

data class CalendarRecord (
  @Field
  var id: String? = null,
  @Field
  val title: String? = null,
  @Field
  val name: String? = null,
  @Field
  val source: Source? = null,
  @Field
  val color: String? = null,
  @Field
  val isVisible: Boolean = true,
  @Field
  val isSynced: Boolean = true,
  @Field
  val timeZone: String? = null,
  @Field
  val isPrimary: Boolean = false,
  @Field
  val entityType: CalendarEntity? = null,
  @Field
  val allowsModifications: Boolean = true,
  @Field
  val allowedAvailabilities: List<String> = emptyList(),
  @Field
  var ownerAccount: String? = null,
) : Record

enum class CalendarEntity(val value: String) {
  EVENT("event"), REMINDER("reminder")
}

data class Source (
  @Field
  val id: String? = null,
  @Field
  val type: String? = null,
  @Field
  val name: String? = null,
  @Field
  val isLocalAccount: Boolean = false
) : Record
