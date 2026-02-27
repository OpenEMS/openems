import { TranslateService } from "@ngx-translate/core";
import { JsCalendar } from "src/app/shared/components/schedule/js-calendar-task";
import { ControllerEvseSingleShared } from "../../shared/shared";
import { Mode } from "../chargemode/chargemode";

export class EvseManualPayload extends JsCalendar.OpenEMSPayload<Mode> {
    public override readonly clazz: string = "Manual";
    constructor(mode: Mode | null = null) {
        super();
        this.value = mode;
    }

    public toOpenEMSPayload(): {} {
        return {
            "openems.io:payload": {
                class: this.clazz,
                mode: this.value,
            },
        };
    }

    public override update(payload: JsCalendar.OpenEMSPayload<Mode>, task: JsCalendar.Task<ReturnType<typeof this.toOpenEMSPayload>>) {
        const taskPayload = "openems.io:payload" in task ? task["openems.io:payload"] as { class: string, mode: string } : null;
        const value = taskPayload != null && "mode" in taskPayload ? taskPayload["mode"] : null;
        const clazz = taskPayload != null && "class" in taskPayload ? taskPayload["class"] : null;
        if (payload.clazz == clazz && value != null) {
            payload.setValue(value as Mode);
        }
        return payload;
    }

    public override toPayloadText<T extends { mode?: string | null; }>(translate: TranslateService): JsCalendar.TaskParser<T> {
        return (value: JsCalendar.Task<T>) => {
            if (value == null) {
                return null;
            }
            return ControllerEvseSingleShared.CONVERT_TO_MODE_LABEL(translate)(value?.["openems.io:payload"]?.mode ?? null);
        };
    }
}
