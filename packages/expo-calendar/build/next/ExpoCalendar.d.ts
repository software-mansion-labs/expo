import { NativeModule, PermissionResponse } from 'expo-modules-core';
import { ExpoCalendar, ExpoCalendarAttendee, ExpoCalendarEvent, ExpoCalendarReminder } from './ExpoCalendar.types';
import { Calendar, EntityTypes, Source } from '../Calendar';
declare class ExpoCalendarNextModule extends NativeModule {
    ExpoCalendar: typeof ExpoCalendar;
    ExpoCalendarEvent: typeof ExpoCalendarEvent;
    ExpoCalendarAttendee: typeof ExpoCalendarAttendee;
    ExpoCalendarReminder: typeof ExpoCalendarReminder;
    getDefaultCalendar(): ExpoCalendar;
    getCalendars(type?: EntityTypes): Promise<ExpoCalendar[]>;
    createCalendar(details: Partial<Calendar>): ExpoCalendar;
    listEvents(calendars: ExpoCalendar[], startDate: string | Date, endDate: string | Date): Promise<ExpoCalendarEvent[]>;
    requestCalendarPermissionsAsync(): Promise<PermissionResponse>;
    getCalendarPermissionsAsync(): Promise<PermissionResponse>;
    requestRemindersPermissionsAsync(): Promise<PermissionResponse>;
    getRemindersPermissionsAsync(): Promise<PermissionResponse>;
    getSources(): Source[];
}
declare const _default: ExpoCalendarNextModule;
export default _default;
//# sourceMappingURL=ExpoCalendar.d.ts.map