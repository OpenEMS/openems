import { TranslateService } from "@ngx-translate/core";
import { JsCalendar } from "src/app/shared/components/schedule/js-calendar-task";
import { OneTask } from "src/app/shared/jsonrpc/response/getOneTasksResponse";
import { ControllerEvseSingleShared } from "../../shared/shared";
import { Mode } from "../chargemode/chargemode";

export class EvseManualPayload extends JsCalendar.OpenEMSPayload<{ mode: Mode, class: string }> {

    public override toOneTasks<T extends { mode?: string | null; }>(task: OneTask<T>, translate: TranslateService): string | null {
        return ControllerEvseSingleShared.CONVERT_TO_MODE_LABEL(translate)(task?.payload?.mode ?? null);
    }

    public toOpenEMSPayload(): {} {
        return {
            "openems.io:payload": this.value,
        };
    }

    public override update(payload: JsCalendar.OpenEMSPayload<{ mode: Mode, class: string }>, task: JsCalendar.Task<ReturnType<typeof this.toOpenEMSPayload>>) {
        const taskPayload = "openems.io:payload" in task ? task["openems.io:payload"] as { class: string, mode: string } : null;
        const value = taskPayload != null && "mode" in taskPayload ? taskPayload["mode"] : null;
        const clazz = taskPayload != null && "class" in taskPayload ? taskPayload["class"] : null;
        if (value != null && clazz != null) {
            payload.setValue({ class: clazz, mode: value as Mode });
        }
        return payload;
    }

    public override toPayloadText<T extends { mode?: string | null; }>(translate: TranslateService): JsCalendar.Types.TaskParser<T> {
        return (value: JsCalendar.Task<T>) => {
            if (value == null) {
                return null;
            }
            return ControllerEvseSingleShared.CONVERT_TO_MODE_LABEL(translate)(value?.["openems.io:payload"]?.mode ?? null);
        };
    }
}
