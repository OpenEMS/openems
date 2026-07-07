import { TranslateService } from "@ngx-translate/core";
import { Mode } from "src/app/edge/live/Controller/Heat/settings/settings";
import { JsCalendar } from "src/app/shared/components/schedule/js-calendar-task";
import { OneTask } from "src/app/shared/jsonrpc/response/getOneTasksResponse";
import { CONVERT_TO_MODE_LABEL } from "../shared/shared";

export class HeatManualPayload extends JsCalendar.OpenEMSPayload<{ mode: Mode }> {

    public override toOneTasks<T extends { mode?: string | null; }>(task: OneTask<T>, translate: TranslateService): string | null {
        return CONVERT_TO_MODE_LABEL(translate)(task?.payload?.mode ?? null);
    }

    public toOpenEMSPayload(): {} {
        return {
            "openems.io:payload": this.value,
        };
    }

    public override update(payload: JsCalendar.OpenEMSPayload<{ mode: Mode }>, task: JsCalendar.Task<ReturnType<typeof this.toOpenEMSPayload>>) {
        const taskPayload = "openems.io:payload" in task ? task["openems.io:payload"] as { mode: string } : null;
        const value = taskPayload != null && "mode" in taskPayload ? taskPayload["mode"] : null;
        if (value != null) {
            payload.setValue({ mode: value as Mode });
        }
        return payload;
    }

    public override toPayloadText<T extends { mode?: string | null; }>(translate: TranslateService): JsCalendar.Types.TaskParser<T> {
        return (value: JsCalendar.Task<T>) => {
            if (value == null) {
                return null;
            }
            return CONVERT_TO_MODE_LABEL(translate)(value?.["openems.io:payload"]?.mode ?? null);
        };
    }
}
