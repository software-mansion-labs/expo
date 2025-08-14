package expo.modules.calendar.next

import android.Manifest
import android.database.Cursor
import android.provider.CalendarContract
import android.util.Log
import expo.modules.calendar.CalendarModule.Companion.TAG
import expo.modules.calendar.CalendarUtils
import expo.modules.calendar.ModuleDestroyedException
import expo.modules.calendar.dialogs.CreateEventContract
import expo.modules.calendar.dialogs.CreateEventIntentResult
import expo.modules.calendar.dialogs.CreatedEventOptions
import expo.modules.calendar.dialogs.ViewEventContract
import expo.modules.calendar.dialogs.ViewEventIntentResult
import expo.modules.calendar.dialogs.ViewedEventOptions
import expo.modules.calendar.findCalendarsQueryParameters
import expo.modules.kotlin.Promise
import expo.modules.kotlin.activityresult.AppContextActivityResultLauncher
import expo.modules.kotlin.apifeatures.EitherType
import expo.modules.kotlin.exception.Exceptions
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import expo.modules.calendar.next.records.EventRecord
import expo.modules.calendar.next.records.RecurringEventOptions
import expo.modules.kotlin.functions.Coroutine
import kotlinx.coroutines.cancel
import expo.modules.calendar.next.records.AttendeeRecord

class CalendarNextModule : Module() {
  private val moduleCoroutineScope = CoroutineScope(Dispatchers.Default)
  public val contentResolver
    get() = (appContext.reactContext ?: throw Exceptions.ReactContextLost()).contentResolver

  private lateinit var createEventLauncher: AppContextActivityResultLauncher<CreatedEventOptions, CreateEventIntentResult>
  private lateinit var viewEventLauncher: AppContextActivityResultLauncher<ViewedEventOptions, ViewEventIntentResult>

  @OptIn(EitherType::class)
  override fun definition() = ModuleDefinition {
    Name("CalendarNext")

    RegisterActivityContracts {
      createEventLauncher = registerForActivityResult(
        CreateEventContract()
      )
      viewEventLauncher = registerForActivityResult(
        ViewEventContract()
      )
    }

    OnDestroy {
      try {
        moduleCoroutineScope.cancel(ModuleDestroyedException())
      } catch (e: IllegalStateException) {
        Log.e(TAG, "The scope does not have a job in it")
      }
    }

    AsyncFunction("getCalendars") { type: String?, promise: Promise ->
      withPermissions(promise) {
        if (type != null && type == "reminder") {
          promise.reject("E_CALENDARS_NOT_FOUND", "Calendars of type `reminder` are not supported on Android", null)
          return@withPermissions
        }
        launchAsyncWithModuleScope(promise) {
          try {
            val expoCalendars = findExpoCalendars()
            promise.resolve(expoCalendars)
          } catch (e: Exception) {
            promise.reject("E_CALENDARS_NOT_FOUND", "Calendars could not be found", e)
          }
        }
      }
    }

    Class(ExpoCalendar::class) {
      Constructor { id: String ->
        ExpoCalendar(appContext, id)
      }

      Property("id") { expoCalendar: ExpoCalendar ->
        expoCalendar.id
      }

      Property("title") { expoCalendar: ExpoCalendar ->
        expoCalendar.title
      }

      Property("isPrimary") { expoCalendar: ExpoCalendar ->
        expoCalendar.isPrimary
      }

      Property("name") { expoCalendar: ExpoCalendar ->
        expoCalendar.name
      }

      Property("color") { expoCalendar: ExpoCalendar ->
        expoCalendar.color
      }

      Property("ownerAccount") { expoCalendar: ExpoCalendar ->
        expoCalendar.ownerAccount
      }

      Property("timeZone") { expoCalendar: ExpoCalendar ->
        expoCalendar.timeZone
      }

      Property("isVisible") { expoCalendar: ExpoCalendar ->
        expoCalendar.isVisible
      }

      Property("isSynced") { expoCalendar: ExpoCalendar ->
        expoCalendar.isSynced
      }

      Property("allowsModifications") { expoCalendar: ExpoCalendar ->
        expoCalendar.allowsModifications
      }

      AsyncFunction("listEvents") { expoCalendar: ExpoCalendar, startDate: Any, endDate: Any, promise: Promise ->
        withPermissions(promise) {
          launchAsyncWithModuleScope(promise) {
            if (expoCalendar.id == null) {
              promise.reject("E_EVENTS_NOT_FOUND", "Calendar doesn't exist", Exception())
            }
            try {
              val expoCalendarEvents = expoCalendar.getEvents(startDate, endDate)
              promise.resolve(expoCalendarEvents)
            } catch (e: Exception) {
              promise.reject("E_EVENTS_NOT_FOUND", "Events could not be found", e)
            }
          }
        }
      }

      AsyncFunction("createEvent") { expoCalendar: ExpoCalendar, record: EventRecord, promise: Promise ->
        withPermissions(promise) {
          launchAsyncWithModuleScope(promise) {
            try {
              val expoCalendarEvent = expoCalendar.createEvent(record)
              promise.resolve(expoCalendarEvent)
            } catch (e: Exception) {
              promise.reject("E_EVENT_NOT_CREATED", "Event could not be created", e)
            }
          }
        }
      }
    }

    Class(ExpoCalendarEvent::class) {
      Constructor { id: String ->
        ExpoCalendarEvent(appContext)
      }

      AsyncFunction("createAttendee") { expoCalendarEvent: ExpoCalendarEvent, record: AttendeeRecord, promise: Promise ->
        withPermissions(promise) {
          launchAsyncWithModuleScope(promise) {
            try {
              val attendee = expoCalendarEvent.createAttendee(record)
              promise.resolve(attendee)
            } catch (e: Exception) {
              promise.reject("E_ATTENDEE_NOT_CREATED", "Attendee could not be created", e)
            }
          }
        }
      }

      Property("id") { expoCalendarEvent: ExpoCalendarEvent ->
        expoCalendarEvent.eventRecord?.id
      }

      Property("calendarId") { expoCalendarEvent: ExpoCalendarEvent ->
        expoCalendarEvent.eventRecord?.calendarId
      }

      Property("title") { expoCalendarEvent: ExpoCalendarEvent ->
        expoCalendarEvent.eventRecord?.title
      }

      Property("notes") { expoCalendarEvent: ExpoCalendarEvent ->
        expoCalendarEvent.eventRecord?.notes
      }

      Property("recurrenceRule") { expoCalendarEvent: ExpoCalendarEvent ->
        expoCalendarEvent.eventRecord?.recurrenceRule
      }

      Property("startDate") { expoCalendarEvent: ExpoCalendarEvent ->
        expoCalendarEvent.eventRecord?.startDate
      }

      Property("endDate") { expoCalendarEvent: ExpoCalendarEvent ->
        expoCalendarEvent.eventRecord?.endDate
      }

      Property("allDay") { expoCalendarEvent: ExpoCalendarEvent ->
        expoCalendarEvent.eventRecord?.allDay
      }

      Property("location") { expoCalendarEvent: ExpoCalendarEvent ->
        expoCalendarEvent.eventRecord?.location
      }

      Property("timeZone") { expoCalendarEvent: ExpoCalendarEvent ->
        expoCalendarEvent.eventRecord?.timeZone
      }

      Property("endTimeZone") { expoCalendarEvent: ExpoCalendarEvent ->
        expoCalendarEvent.eventRecord?.endTimeZone
      }

      Property("availability") { expoCalendarEvent: ExpoCalendarEvent ->
        expoCalendarEvent.eventRecord?.availability?.value
      }

      Property("status") { expoCalendarEvent: ExpoCalendarEvent ->
        expoCalendarEvent.eventRecord?.status?.value
      }

      Property("organizerEmail") { expoCalendarEvent: ExpoCalendarEvent ->
        expoCalendarEvent.eventRecord?.organizerEmail
      }

      Property("accessLevel") { expoCalendarEvent: ExpoCalendarEvent ->
        expoCalendarEvent.eventRecord?.accessLevel?.value
      }

      Property("guestsCanModify") { expoCalendarEvent: ExpoCalendarEvent ->
        expoCalendarEvent.eventRecord?.guestsCanModify
      }

      Property("guestsCanInviteOthers") { expoCalendarEvent: ExpoCalendarEvent ->
        expoCalendarEvent.eventRecord?.guestsCanInviteOthers
      }

      Property("guestsCanSeeGuests") { expoCalendarEvent: ExpoCalendarEvent ->
        expoCalendarEvent.eventRecord?.guestsCanSeeGuests
      }

      Property("originalId") { expoCalendarEvent: ExpoCalendarEvent ->
        expoCalendarEvent.eventRecord?.originalId
      }

      AsyncFunction("openInCalendarAsync") Coroutine { expoCalendarEvent: ExpoCalendarEvent, rawParams: ViewedEventOptions ->
        val eventId = expoCalendarEvent.eventRecord?.id;
        if (eventId == null) {
          throw Exception("Event id is null")
        }
        val params = ViewedEventOptions(
          id = eventId,
          startNewActivityTask = rawParams.startNewActivityTask
        )
        val result = viewEventLauncher.launch(params)
        return@Coroutine result
      }

      AsyncFunction("editInCalendarAsync") Coroutine { expoCalendarEvent: ExpoCalendarEvent, rawParams: ViewedEventOptions? ->
        val eventId = expoCalendarEvent.eventRecord?.id;
        if (eventId == null) {
          throw Exception("Event id is null")
        }
        val params = ViewedEventOptions(
          id = eventId,
          startNewActivityTask = rawParams?.startNewActivityTask ?: true
        )
        viewEventLauncher.launch(params)
        val editResult = CreateEventIntentResult()
        return@Coroutine editResult
      }

      AsyncFunction("getAttendees") { expoCalendarEvent: ExpoCalendarEvent, _: RecurringEventOptions, promise: Promise ->
        // TODO: Support recurringEventOptions. Legacy Calendar API doesn't support it, check if we can support it.
        launchAsyncWithModuleScope(promise) {
          val attendees = expoCalendarEvent.getAttendees()
          promise.resolve(attendees)
        }
      }

      Function("update") { expoCalendarEvent: ExpoCalendarEvent, eventRecord: EventRecord, _: Any, nullableFields: List<String> ->
        val updatedRecord = expoCalendarEvent.eventRecord?.getUpdatedRecord(eventRecord, nullableFields)
        if (updatedRecord == null) {
          throw Exception("Event record is null")
        }
        expoCalendarEvent.saveEvent(updatedRecord)
        expoCalendarEvent.eventRecord = updatedRecord
      }

      Function("delete") { expoCalendarEvent: ExpoCalendarEvent, recurringEventOptions: RecurringEventOptions ->
        expoCalendarEvent.deleteEvent(recurringEventOptions)
      }
    }

    Class(ExpoCalendarAttendee::class) {
      Constructor { id: String ->
        ExpoCalendarAttendee(appContext)
      }

      Property("id") { expoCalendarAttendee: ExpoCalendarAttendee ->
        expoCalendarAttendee.attendeeRecord?.id
      }

      Property("name") { expoCalendarAttendee: ExpoCalendarAttendee ->
        expoCalendarAttendee.attendeeRecord?.name
      }

      Property("role") { expoCalendarAttendee: ExpoCalendarAttendee ->
        expoCalendarAttendee.attendeeRecord?.role?.value
      }

      Property("status") { expoCalendarAttendee: ExpoCalendarAttendee ->
        expoCalendarAttendee.attendeeRecord?.status?.value
      }

      Property("type") { expoCalendarAttendee: ExpoCalendarAttendee ->
        expoCalendarAttendee.attendeeRecord?.type?.value
      }

      Property("email") { expoCalendarAttendee: ExpoCalendarAttendee ->
        expoCalendarAttendee.attendeeRecord?.email
      }

      AsyncFunction("update") { expoCalendarAttendee: ExpoCalendarAttendee, attendeeRecord: AttendeeRecord, promise: Promise ->
        withPermissions(promise) {
          launchAsyncWithModuleScope(promise) {
            try {
              val updatedRecord = expoCalendarAttendee.attendeeRecord?.getUpdatedRecord(attendeeRecord, emptyList())
              if (updatedRecord == null) {
                throw Exception("Event record is null")
              }
              expoCalendarAttendee.saveAttendee(updatedRecord)
              expoCalendarAttendee.attendeeRecord = updatedRecord
              promise.resolve()
            } catch (e: Exception) {
              promise.reject("E_ATTENDEE_NOT_UPDATED", "Attendee could not be updated", e)
            }
          }
        }
      }

      AsyncFunction("delete") { expoCalendarAttendee: ExpoCalendarAttendee, promise: Promise ->
        withPermissions(promise) {
          launchAsyncWithModuleScope(promise) {
            try {
              val successful = expoCalendarAttendee.deleteAttendee()
              if (successful) {
                promise.resolve(null)
              } else {
                promise.reject("E_ATTENDEE_NOT_DELETED", "Attendee could not be deleted", null)
              }
            } catch (e: Exception) {
              promise.reject("E_ATTENDEE_NOT_DELETED", "An error occurred while deleting attendee", e)
            }
          }
        }
      }
    }

    Class(ExpoCalendarReminder::class) {
      Constructor { id: String ->
        ExpoCalendarReminder(id)
      }
    }
  }

  @Throws(SecurityException::class)
  private fun findExpoCalendars(): List<ExpoCalendar> {
    val uri = CalendarContract.Calendars.CONTENT_URI
    val cursor = contentResolver.query(uri, findCalendarsQueryParameters, null, null, null)
    requireNotNull(cursor) { "Cursor shouldn't be null" }
    return cursor.use(::serializeExpoCalendars)
  }

  private fun serializeExpoCalendars(cursor: Cursor): List<ExpoCalendar> {
    val results: MutableList<ExpoCalendar> = ArrayList()
    while (cursor.moveToNext()) {
      results.add(ExpoCalendar(appContext, cursor))
    }
    return results
  }

  private inline fun launchAsyncWithModuleScope(promise: Promise, crossinline block: () -> Unit) {
    moduleCoroutineScope.launch {
      try {
        block()
      } catch (e: ModuleDestroyedException) {
        promise.reject("E_CALENDAR_MODULE_DESTROYED", "Module destroyed, promise canceled", null)
      }
    }
  }

  private fun checkPermissions(promise: Promise): Boolean {
    if (appContext.permissions?.hasGrantedPermissions(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR) != true) {
      promise.reject("E_MISSING_PERMISSIONS", "CALENDAR permission is required to do this operation.", null)
      return false
    }
    return true
  }

  private inline fun withPermissions(promise: Promise, block: () -> Unit) {
    if (!checkPermissions(promise)) {
      return
    }
    block()
  }
}
