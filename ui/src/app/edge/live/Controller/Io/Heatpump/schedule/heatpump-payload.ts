import { TranslateService } from "@ngx-translate/core";
import { JsCalendar } from "src/app/shared/components/schedule/js-calendar-task";
import { OneTask } from "src/app/shared/jsonrpc/response/getOneTasksResponse";
import { BaseMode, CONVERT_TO_BASE_MODE_LABEL } from "../shared/shared";

export class HeatpumpPayload extends JsCalendar.OpenEMSPayload<{ baseMode: BaseMode }> {
    public override toOneTasks<T extends { baseMode?: BaseMode | null }>(
        task: OneTask<T>,
        translate: TranslateService,
    ): string | null {
        return CONVERT_TO_BASE_MODE_LABEL(translate)(task?.payload?.baseMode ?? null);
    }

    public override toOpenEMSPayload(): {} {
        return { "openems.io:payload": this.value };
    }

    public override update(
        payload: JsCalendar.OpenEMSPayload<{ baseMode: BaseMode }>,
        task: JsCalendar.Task<ReturnType<typeof this.toOpenEMSPayload>>,
    ) {
        const taskPayload =
            "openems.io:payload" in task ? (task["openems.io:payload"] as { baseMode: BaseMode }) : null;
        const value = taskPayload != null && "baseMode" in taskPayload ? taskPayload["baseMode"] : null;
        if (value != null) {
            payload.setValue({ baseMode: value });
        }
        return payload;
    }

    public override toPayloadText<T extends { baseMode?: BaseMode | null }>(
        translate: TranslateService,
    ): JsCalendar.Types.TaskParser<T> {
        return (value: JsCalendar.Task<T>) => {
            if (value == null) {
                return null;
            }
            return CONVERT_TO_BASE_MODE_LABEL(translate)(value?.["openems.io:payload"]?.baseMode ?? null);
        };
    }
}
