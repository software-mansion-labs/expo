package expo.modules.calendar.next.records

import expo.modules.core.arguments.ReadableArguments
import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record
import java.io.Serializable

class CalendarRecordNext (
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

) : Record, Serializable {
  companion object {
    fun fromReadableArguments(args: ReadableArguments): CalendarRecordNext {
      return CalendarRecordNext(
        id = if (args.containsKey("id")) args.getString("id") else null,
        title = if (args.containsKey("title")) args.getString("title") else null,
        isPrimary = if(args.containsKey("isPrimary")) args.getBoolean("isPrimary") else false,
        name = if (args.containsKey("name")) args.getString("name") else null,
        source = if (args.containsKey("source")) {
          val sourceArgs = args.getArguments("source")
          Source.fromReadableArguments(sourceArgs)
        } else null,
        color = if (args.containsKey("color")) args.getString("color") else null,
        entityType = if (args.containsKey("entityType")) {
          when (args.getString("entityType")) {
            "event" -> CalendarEntity.EVENT
            "reminder" -> CalendarEntity.REMINDER
            else -> null
          }
        } else null,
        allowsModifications = if (args.containsKey("allowsModifications")) args.getBoolean("allowsModifications") else true,
        allowedAvailabilities = if (args.containsKey("allowedAvailabilities")) {
          args.getList("allowedAvailabilities").mapNotNull { it as? String }
        } else emptyList(),
        timeZone = if (args.containsKey("timeZone")) args.getString("timeZone") else null,
        isVisible = if (args.containsKey("isVisible")) args.getBoolean("isVisible") else true,
        isSynced = if (args.containsKey("isSynced")) args.getBoolean("isSynced") else true
      )
    }
  }
}

enum class CalendarEntity {
  EVENT, REMINDER
}

class Source (
  @Field
  val id: String? = null,
  @Field
  val type: String? = null,
  @Field
  val name: String? = null,
  @Field
  val isLocalAccount: Boolean = false
) : Record, Serializable {
  companion object {
    fun fromReadableArguments(args: ReadableArguments): Source {
      return Source(
        id = if (args.containsKey("id")) args.getString("id") else null,
        type = if (args.containsKey("type")) args.getString("type") else null,
        name = if (args.containsKey("name")) args.getString("name") else null,
        isLocalAccount = if (args.containsKey("isLocalAccount")) args.getBoolean("isLocalAccount") else false
      )
    }
  }
}
