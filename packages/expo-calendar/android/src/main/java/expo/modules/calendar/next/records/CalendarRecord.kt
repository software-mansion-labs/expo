package expo.modules.calendar.next.records

import android.database.Cursor
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
  @Field
  var cursor: Cursor? = null

) : Record

enum class CalendarEntity {
  EVENT, REMINDER
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
