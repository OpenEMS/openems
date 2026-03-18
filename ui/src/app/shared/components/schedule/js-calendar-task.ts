import { inject, Injector } from "@angular/core";
import { TZDate } from "@date-fns/tz";
import { TranslateService } from "@ngx-translate/core";
import { ValidationResult } from "json-schema";
import * as Duration from "tinyduration";
import { parse } from "tinyduration";
import { JsonrpcRequest } from "src/app/shared/jsonrpc/base";
import { OneTask } from "../../jsonrpc/response/getOneTasksResponse";
import { Role } from "../../type/role";
import { EmptyObj, TIntRange } from "../../type/utility";
import { DateTimeUtils } from "../../utils/datetime/datetime-utils";
import { Edge } from "../edge/edge";
import { TaskFormWeeklyComponent } from "./form/weekly/weekly";

export namespace JsCalendar {

    export namespace Utils {
        export function calculateEndTimeFromDuration(startDateString: string | null, isoDuration: string | null): string | null {

            if (startDateString === null || isoDuration === null) {
                return null;
            }
            const date = new TZDate(startDateString, DateTimeUtils.getLocaleTimeZone());
            const { hours, minutes, seconds } = parse(isoDuration) || {};
            if (hours == null && minutes == null && seconds == null) {
                return null;
            }

            date.setHours(date.getHours() + (hours ?? 0));
            date.setMinutes(date.getMinutes() + (minutes ?? 0));
            date.setSeconds(date.getSeconds() + (seconds ?? 0));

            return date.toISOString();
        }


        export function computeIsoDuration(startDate: Date | null, endDate: Date | null): { duration: JsCalendar.Task["duration"] } | EmptyObj {

            if (startDate == null || endDate == null) {
                return {};
            }

            const MS_TO_MINUTES = 60000;
            const durationMs = endDate.getTime() - startDate.getTime();
            const totalMinutes = Math.floor(durationMs / MS_TO_MINUTES);
            const days = Math.floor(totalMinutes / (24 * 60));
            const hours = Math.floor((totalMinutes % (24 * 60)) / 60);
            const minutes = totalMinutes % 60;
            const seconds = minutes < 60 ? 0 : minutes % 60;

            return {
                duration: Duration.serialize({
                    days: days,
                    hours: hours,
                    minutes: minutes,
                    seconds: seconds,
                }),
            };
        }

        export function formatIsoLocalDateTime(date: Date | null): string | null {
            if (date == null) {
                return null;
            }
            return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, "0")}-${date.getDate().toString().padStart(2, "0")}T${date.getHours().toString().padStart(2, "0")}:${date.getMinutes().toString().padStart(2, "0")}:00`;
        }
    }


    export class UpdateTaskRequest extends JsonrpcRequest {

        public static METHOD: string = "updateTask";
        public constructor(
            public override readonly params: {
                task: Task,
            },
        ) {
            super(UpdateTaskRequest.METHOD, params);
        }
    }

    export class AddTaskRequest extends JsonrpcRequest {

        public static METHOD: string = "addTask";
        public constructor(
            public override readonly params: {
                task: Task,
            },
        ) {
            super(AddTaskRequest.METHOD, params);
        }
    }

    export interface Task<Payload = {}> {
        "@type": "Task";
        "start": string;              // e.g. "08:30:00"
        /** JsCalendar format accepts multiple recurrenceRules per task  */
        "recurrenceRules": Types.RecurrenceRule[];

        "uid"?: string;
        "updated"?: string;            // ISO timestamp string
        "duration"?: string;           // e.g. "PT15M"

        /** Differentiates between controllers/components */
        ["openems.io:payload"]?: Payload;
    }

    export namespace Types {

        export type RecurrenceRule =
            { frequency: "daily" }
            | {
                frequency: "weekly",
                byDay: WeekDayKeys[]
            }
            | {
                frequency: "monthly",
                byDay: { day: WeekDayKeys | null, nthOfPeriod: TIntRange<1, 5> | null }[]
            }
            | {
                // TODO: to be implemented
                frequency: "yearly",
            };

        export type UpdateTask<Payload extends Record<string, unknown> = {}> = Task<Payload> & {
            "uid": string
        };

        export type WeekDayKeys = ReturnType<typeof TaskFormWeeklyComponent.WEEK_DAYS>[number]["key"];

        export type RuleOf<F extends string> = Extract<
            JsCalendar.Task["recurrenceRules"][number],
            { frequency: F } & object
        >;

        export type TaskParser<Payload extends object = {}> = (value: Task<Payload>) => string | null;
    }


    export abstract class OpenEMSPayload<T = string> {
        protected value: T | null = null;
        protected injector: Injector | null = null;

        constructor() {
            this.injector = inject(Injector);
        }

        /**
         * Checks if user can add or update tasks.
         *
         * @param edge the edge
         * @returns true if user can edit a task
         */
        public canWrite(edge: Edge | null): boolean {
            return edge?.roleIsAtLeast(Role.OWNER) ?? false;
        }

        public setValue(value: typeof this.value) {
            this.value = value;
        }

        /**
         * Updates the task with new payload.
         *
         * @param payload the payload to use for the task update
         * @param task the task to update
         * @returns the updated payload.
         */
        public update(el: OpenEMSPayload<T> | null, task: Task) {
            if (el == null) {
                return;
            }
            return el;
        }

        /**
         * Takes a payload and formats it to a payload text.
         *
         * @param translate the translate service
         * @returns a string representing the controller specific payload text.
         */
        public toPayloadText<T extends object = {}>(translate: TranslateService): Types.TaskParser<T> {
            return (value: Task<T>) => value != null ? value.toString() : null;
        }

        /**
         * Validates the value.
         *
         * @param translate the translate service
         * @returns a validation result
         */
        public validator(translate: TranslateService): ValidationResult {
            const isValid = this.value != null;
            if (isValid) {
                return { errors: [], valid: true };
            }

            return { errors: [{ message: translate.instant("JS_SCHEDULE.ADD_ERROR"), property: this?.value?.toString() ?? "" }], valid: false };
        };

        /**
         * Formats a value to a OpenEMS payload.
         */
        public abstract toOpenEMSPayload(): { "openems.io:payload": Pick<Task<T>, "openems.io:payload"> } | EmptyObj;

        /**
         * Formats a one tasks payload to a display string.
         *
         * @param oneTask the one task to display
         * @param translate the translate service
         */
        public abstract toOneTasks<Payload extends Record<string, unknown> = {}>(oneTask: OneTask<Payload>, translate: TranslateService): string | null;
    };

    export class BaseOpenEMSPayload extends OpenEMSPayload<string | number | boolean | null> {
        protected override value: string | number | boolean | null = null;

        public override toOneTasks(oneTask: OneTask, translate: TranslateService): string | null {
            return null;
        }
        public override validator(): ValidationResult {
            return { errors: [], valid: true };
        };
        public override toOpenEMSPayload(): {} {
            return {};
        }
    }

    export type ScheduleVM = {
        uid: string;
        start: string;
        end: string | null;
        durationText: string;
        recurrenceText: string;
        payloadText?: string;
        recurrenceRules: Task["recurrenceRules"],
    };

    export interface GetAllTasksResponse {
        jsonrpc: "2.0";
        id: string;
        result: {
            tasks: Task[];
        };
    }
}

