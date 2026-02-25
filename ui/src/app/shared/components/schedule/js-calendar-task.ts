import { TZDate } from "@date-fns/tz";
import { TranslateService } from "@ngx-translate/core";
import { ValidationResult } from "json-schema";
import * as Duration from "tinyduration";
import { parse } from "tinyduration";
import { JsonrpcRequest } from "src/app/shared/jsonrpc/base";
import { Role } from "../../type/role";
import { EmptyObj, TIntRange } from "../../type/utility";
import { DateTimeUtils } from "../../utils/datetime/datetime-utils";
import { Edge } from "../edge/edge";
import { WeekDayKeys } from "./form/monthly/monthly";

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

    export interface Task<Payload extends object = {}> {
        "@type": "Task";
        "start": string;              // e.g. "08:30:00"
        /** JsCalendar format accepts multiple recurrenceRules per task  */
        "recurrenceRules": { frequency: "daily" | "weekly" | "monthly" | "yearly", byDay?: { day: WeekDayKeys | null, nthOfPeriod: TIntRange<1, 5> | null }[] }[];

        "uid"?: string;
        "updated"?: string;            // ISO timestamp string
        "duration"?: string;           // e.g. "PT15M"

        /** Differentiates between controllers/components */
        ["openems.io:payload"]?: Payload;
    }

    export type UpdateTask<Payload extends Record<string, unknown> = {}> = Task<Payload> & {
        "uid": string
    };

    export type TaskParser<Payload extends object = {}> = (value: Task<Payload>) => string | null;

    export abstract class OpenEMSPayload<T = string> {
        protected value: T | null = null;
        public abstract readonly clazz: string | null;

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
        public toPayloadText<T extends object = {}>(translate: TranslateService): TaskParser<T> {
            return (value: Task<T>) => value != null ? value.toString() : null;
        }

        /**
         * Validates the value.
         *
         * @param translate the translate service
         * @returns a validation result
         */
        public validator(translate: TranslateService): ValidationResult {
            const isValid = this.value != null && this.clazz != null;
            if (isValid) {
                return { errors: [], valid: true };
            }

            return { errors: [{ message: translate.instant("JS_SCHEDULE.VALIDATION_ERROR_3"), property: this.clazz ?? "" }], valid: false };
        };

        /**
         * Formats a value to a OpenEMS payload.
         */
        public abstract toOpenEMSPayload(): { "openems.io:payload": Pick<Task, "openems.io:payload"> } | EmptyObj;
    };

    export class BaseOpenEMSPayload extends OpenEMSPayload<string | number | boolean | null> {
        public override clazz = null;
        protected override value: string | number | boolean | null = null;

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

