package expo.modules.calendar.next

import android.Manifest
import android.content.ContentUris
import android.database.Cursor
import android.provider.CalendarContract
import expo.modules.calendar.dialogs.CreateEventContract
import expo.modules.calendar.dialogs.CreateEventIntentResult
import expo.modules.calendar.dialogs.CreatedEventOptions
import expo.modules.calendar.dialogs.ViewEventContract
import expo.modules.calendar.dialogs.ViewEventIntentResult
import expo.modules.calendar.dialogs.ViewedEventOptions
import expo.modules.calendar.findCalendarByIdQueryFields
import expo.modules.calendar.findCalendarsQueryParameters
import expo.modules.calendar.next.exceptions.AttendeeCouldNotBeCreatedException
import expo.modules.calendar.next.exceptions.AttendeeCouldNotBeDeletedException
import expo.modules.calendar.next.exceptions.AttendeeCouldNotBeUpdatedException
import expo.modules.calendar.next.exceptions.AttendeeNotFoundException
import expo.modules.calendar.next.exceptions.CalendarCouldNotBeCreatedException
import expo.modules.calendar.next.exceptions.CalendarCouldNotBeDeletedException
import expo.modules.calendar.next.exceptions.EventNotFoundException
import expo.modules.calendar.next.exceptions.EventsCouldNotBeCreatedException
import expo.modules.calendar.next.records.AttendeeRecord
import expo.modules.calendar.next.records.CalendarRecord
import expo.modules.calendar.next.records.EventRecord
import expo.modules.calendar.next.records.RecurringEventOptions
import expo.modules.calendar.next.exceptions.CalendarNotFoundException
import expo.modules.calendar.next.exceptions.CalendarNotSupportedException
import expo.modules.calendar.next.exceptions.EventCouldNotBeDeletedException
import expo.modules.calendar.next.exceptions.EventCouldNotBeUpdatedException
import expo.modules.calendar.next.extensions.toCalendarRecord
import expo.modules.calendar.next.extensions.toEventRecord
import expo.modules.calendar.next.permissions.CalendarPermissionsDelegate
import expo.modules.calendar.next.utils.findEvents
import expo.modules.interfaces.permissions.Permissions
import expo.modules.kotlin.Promise
import expo.modules.kotlin.activityresult.AppContextActivityResultLauncher
import expo.modules.kotlin.exception.Exceptions
import expo.modules.kotlin.functions.Coroutine
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CalendarNextModule : Module() {
  private val contentResolver
    get() = (appContext.reactContext ?: throw Exceptions.ReactContextLost()).contentResolver

  private lateinit var createEventLauncher: AppContextActivityResultLauncher<CreatedEventOptions, CreateEventIntentResult>
  private lateinit var viewEventLauncher: AppContextActivityResultLauncher<ViewedEventOptions, ViewEventIntentResult>
  private val permissionsDelegate by lazy {
    CalendarPermissionsDelegate(appContext)
  }
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

    AsyncFunction("getCalendars") Coroutine { type: String? ->
      try {
        permissionsDelegate.requireSystemPermissions(false)
        if (type != null && type == "reminder") {
          throw CalendarNotSupportedException("Calendars of type `reminder` are not supported on Android")
        }
        findExpoCalendars()
      } catch (e: CalendarNotSupportedException) {
        throw e
      } catch (e: Exception) {
        throw CalendarNotFoundException( "Calendars could not be found", e)
      }
    }

    AsyncFunction("getCalendarById") Coroutine { calendarId: String ->
      permissionsDelegate.requireSystemPermissions(false)
      val calendar = findExpoCalendarById(calendarId)
      if (calendar == null) {
        throw CalendarNotFoundException("Calendar with id $calendarId not found")
      }
      calendar
    }

    AsyncFunction("requestCalendarPermissionsAsync") { promise: Promise ->
      Permissions.askForPermissionsWithPermissionsManager(appContext.permissions, promise, Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
    }

    AsyncFunction("listEvents") Coroutine { calendarIds: List<String>, startDate: String, endDate: String ->
      permissionsDelegate.requireSystemPermissions(false)
      try {
        val allEvents = mutableListOf<ExpoCalendarEvent>()
        val cursor = findEvents(contentResolver, startDate, endDate, calendarIds)
        cursor.use {
          while (it.moveToNext()) {
            val event = ExpoCalendarEvent(appContext, eventRecord = cursor.toEventRecord(contentResolver))
            allEvents.add(event)
          }
        }
        allEvents
      } catch (e: Exception) {
        throw EventNotFoundException("Events could not be found", e)
      }
    }

    Function("createCalendar") { calendarRecord: CalendarRecord ->
      permissionsDelegate.requireSystemPermissions(true)
      try {
        val calendarId = ExpoCalendar.saveCalendar(calendarRecord, appContext)
        val newCalendarRecord = calendarRecord.copy(id = calendarId.toString())
        ExpoCalendar(appContext, newCalendarRecord)
      } catch (e: Exception) {
        throw CalendarCouldNotBeCreatedException("Failed to create calendar", e)
      }
    }

    Function("getEventById") { eventId: String ->
      permissionsDelegate.requireSystemPermissions(false)
      val event = ExpoCalendarEvent.findEventById(eventId, appContext)
      if (event == null) {
        throw EventNotFoundException("Event with id $eventId not found")
      }
      return@Function event
    }

    Class(ExpoCalendar::class) {
      Constructor { calendarRecord: CalendarRecord ->
        ExpoCalendar(appContext, calendarRecord)
      }

      Property("id") { expoCalendar: ExpoCalendar ->
        expoCalendar.calendarRecord?.id
      }

      Property("title") { expoCalendar: ExpoCalendar ->
        expoCalendar.calendarRecord?.title
      }

      Property("name") { expoCalendar: ExpoCalendar ->
        expoCalendar.calendarRecord?.name
      }

      Property("source") { expoCalendar: ExpoCalendar ->
        expoCalendar.calendarRecord?.source
      }

      Property("color") { expoCalendar: ExpoCalendar ->
        expoCalendar.calendarRecord?.color?.let { colorInt ->
          String.format("#%06X", 0xFFFFFF and colorInt)
        }
      }

      Property("isVisible") { expoCalendar: ExpoCalendar ->
        expoCalendar.calendarRecord?.isVisible
      }

      Property("isSynced") { expoCalendar: ExpoCalendar ->
        expoCalendar.calendarRecord?.isSynced
      }

      Property("timeZone") { expoCalendar: ExpoCalendar ->
        expoCalendar.calendarRecord?.timeZone
      }

      Property("isPrimary") { expoCalendar: ExpoCalendar ->
        expoCalendar.calendarRecord?.isPrimary
      }

      Property("allowsModifications") { expoCalendar: ExpoCalendar ->
        expoCalendar.calendarRecord?.allowsModifications
      }

      Property("allowedAvailabilities") { expoCalendar: ExpoCalendar ->
        expoCalendar.calendarRecord?.allowedAvailabilities
      }

      Property("allowedReminders") { expoCalendar: ExpoCalendar ->
        expoCalendar.calendarRecord?.allowedReminders
      }

      Property("allowedAttendeeTypes") { expoCalendar: ExpoCalendar ->
        expoCalendar.calendarRecord?.allowedAttendeeTypes
      }

      Property("ownerAccount") { expoCalendar: ExpoCalendar ->
        expoCalendar.calendarRecord?.ownerAccount
      }

      Property("accessLevel") { expoCalendar: ExpoCalendar ->
        expoCalendar.calendarRecord?.accessLevel
      }

      AsyncFunction("listEvents") Coroutine { expoCalendar: ExpoCalendar, startDate: String, endDate: String ->
        permissionsDelegate.requireSystemPermissions(false)
        if (expoCalendar.calendarRecord?.id == null) {
          throw CalendarNotFoundException("Calendar doesn't exist")
        }
        try {
          expoCalendar.getEvents(startDate, endDate)
        } catch (e: Exception) {
          throw EventNotFoundException("Events could not be found", e)
        }
      }

      Function("createEvent") { expoCalendar: ExpoCalendar, record: EventRecord ->
        permissionsDelegate.requireSystemPermissions(true)
        try {
          expoCalendar.createEvent(record)
        } catch (e: Exception) {
          throw EventsCouldNotBeCreatedException("Event could not be created", e)
        }
      }

      Function("update") { expoCalendar: ExpoCalendar, details: CalendarRecord ->
        permissionsDelegate.requireSystemPermissions(true)
        try {
          val updatedRecord = expoCalendar.calendarRecord?.getUpdatedRecord(details)
            ?: throw CalendarNotFoundException("Calendar record is null")
          ExpoCalendar.updateCalendar(updatedRecord, appContext, isNew = false)
          expoCalendar.calendarRecord = updatedRecord
        } catch (e: CalendarNotFoundException) {
          throw e
        } catch (e: Exception) {
          throw CalendarCouldNotBeCreatedException("Failed to update calendar", e)
        }
      }

      Function("delete") { expoCalendar: ExpoCalendar ->
        permissionsDelegate.requireSystemPermissions(true)
        val successful = expoCalendar.deleteCalendar()
        if (!successful) {
          throw CalendarCouldNotBeDeletedException("An error occurred while deleting calendar")
        }
      }
    }

    Class(ExpoCalendarEvent::class) {
      Constructor { id: String ->
        ExpoCalendarEvent(appContext)
      }

      Function("createAttendee") { expoCalendarEvent: ExpoCalendarEvent, record: AttendeeRecord ->
        permissionsDelegate.requireSystemPermissions(true)
        expoCalendarEvent.createAttendee(record)
          ?: throw AttendeeCouldNotBeCreatedException("Attendee could not be created")
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

      Property("alarms") { expoCalendarEvent: ExpoCalendarEvent ->
        expoCalendarEvent.eventRecord?.alarms
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

      Property("instanceId") { expoCalendarEvent: ExpoCalendarEvent ->
        expoCalendarEvent.eventRecord?.instanceId
      }

      AsyncFunction("openInCalendarAsync") Coroutine { expoCalendarEvent: ExpoCalendarEvent, rawParams: ViewedEventOptions ->
        val eventId = expoCalendarEvent.eventRecord?.id
          ?: throw EventNotFoundException("Event id is null");

        val params = ViewedEventOptions(
          id = eventId,
          startNewActivityTask = rawParams.startNewActivityTask
        )
        val result = viewEventLauncher.launch(params)

        return@Coroutine result
      }

      AsyncFunction("editInCalendarAsync") Coroutine { expoCalendarEvent: ExpoCalendarEvent, rawParams: ViewedEventOptions? ->
        val eventId = expoCalendarEvent.eventRecord?.id
          ?: throw EventNotFoundException("Event id is null");
        val params = ViewedEventOptions(
          id = eventId,
          startNewActivityTask = rawParams?.startNewActivityTask ?: true
        )
        viewEventLauncher.launch(params)
        val editResult = CreateEventIntentResult()
        return@Coroutine editResult
      }

      AsyncFunction("getAttendeesAsync") { expoCalendarEvent: ExpoCalendarEvent, promise: Promise ->
        try {
          permissionsDelegate.requireSystemPermissions(false)
          val attendees = expoCalendarEvent.getAttendees()
          promise.resolve(attendees)
        } catch (e: Exception) {
          throw AttendeeNotFoundException("Attendees could not be found", e)
        }
      }

      Function("getOccurrence") { expoCalendarEvent: ExpoCalendarEvent, options: RecurringEventOptions? ->
        permissionsDelegate.requireSystemPermissions(false)
        try {
          expoCalendarEvent.getOccurrence(options)
        } catch (e: Exception) {
          throw EventNotFoundException("Failed to get occurrence", e)
        }
      }

      Function("update") { expoCalendarEvent: ExpoCalendarEvent, eventRecord: EventRecord, nullableFields: List<String> ->
        permissionsDelegate.requireSystemPermissions(true)
        try {
          expoCalendarEvent.saveEvent(eventRecord, nullableFields = nullableFields)
          expoCalendarEvent.reloadEvent()
        } catch (e: Exception) {
          throw EventCouldNotBeUpdatedException("Failed to update event", e)
        }
      }

      Function("delete") { expoCalendarEvent: ExpoCalendarEvent ->
        permissionsDelegate.requireSystemPermissions(true)
        try {
          expoCalendarEvent.deleteEvent()
        } catch (e: Exception) {
          throw EventCouldNotBeDeletedException("Event could not be deleted", e)
        }
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



      Function("update") { expoCalendarAttendee: ExpoCalendarAttendee, attendeeRecord: AttendeeRecord, nullableFields: List<String> ->
        permissionsDelegate.requireSystemPermissions(true)
        try {
          expoCalendarAttendee.saveAttendee(attendeeRecord, nullableFields = nullableFields)
          expoCalendarAttendee.reloadAttendee()
        } catch (e: Exception) {
          throw AttendeeCouldNotBeUpdatedException("Attendee could not be updated", e)
        }
      }

      Function("delete") { expoCalendarAttendee: ExpoCalendarAttendee ->
        permissionsDelegate.requireSystemPermissions(true)
        val successful = expoCalendarAttendee.deleteAttendee()
        if (!successful) {
          throw AttendeeCouldNotBeDeletedException("An error occurred while deleting attendee")
        }
      }
    }

    Class(ExpoCalendarReminder::class) {
      Constructor { id: String ->
        ExpoCalendarReminder(id)
      }
    }
  }

  private suspend fun findExpoCalendars(): List<ExpoCalendar> {
    return withContext(Dispatchers.IO) {
      val uri = CalendarContract.Calendars.CONTENT_URI
      val cursor = contentResolver.query(uri, findCalendarsQueryParameters, null, null, null)
      requireNotNull(cursor) { "Cursor shouldn't be null" }
      cursor.use(::serializeExpoCalendars)
    }
  }

  private fun serializeExpoCalendars(cursor: Cursor): List<ExpoCalendar> {
    val results: MutableList<ExpoCalendar> = ArrayList()
    while (cursor.moveToNext()) {
      results.add(ExpoCalendar(appContext, calendar = cursor.toCalendarRecord()))
    }
    return results
  }

  private suspend fun findExpoCalendarById(calendarID: String): ExpoCalendar? {
    return withContext(Dispatchers.IO) {
      val uri = ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, calendarID.toInt().toLong())
      val cursor = contentResolver.query(
        uri,
        findCalendarByIdQueryFields,
        null,
        null,
        null
      )
      requireNotNull(cursor) { "Cursor shouldn't be null" }
      cursor.use {
        if (it.count > 0) {
          it.moveToFirst()
          ExpoCalendar(appContext, calendar = cursor.toCalendarRecord())
        } else {
          null
        }
      }
    }
  }
}
