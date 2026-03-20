import { Component, model } from "@angular/core";
import { Mode } from "src/app/edge/live/Controller/Evse/pages/chargemode/chargemode";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { AddTaskComponent } from "src/app/shared/components/schedule/add/add-task.component";
import { TaskFormComponent } from "src/app/shared/components/schedule/form/task-form.component";
import { JsCalendarAddTaskComponent } from "src/app/shared/components/schedule/js-calendar-add-task";
import { TSignalValue } from "src/app/shared/type/utility";
import { ControllerEvseSingleShared } from "../../../../shared/shared";
import { EvseManualPayload } from "../../js-calender-utils";

@Component({
    templateUrl: "./add.html",
    imports: [
        AddTaskComponent,
        CommonUiModule,
    ],
})
export class EvseAddTaskComponent extends JsCalendarAddTaskComponent {

    public payload = model<EvseManualPayload>(new EvseManualPayload());
    public allowedPeriods = model<TSignalValue<TaskFormComponent["allowedPeriods"]>>(["daily", "monthly"]);
    protected modeOptions: { value: Mode, label: string }[] = Object.values(Mode).map(mode => ({
        value: mode,
        label: ControllerEvseSingleShared.CONVERT_TO_MODE_LABEL(this.translate)(mode),
    }));

    setValue(event: CustomEvent) {
        this.payload.set(new EvseManualPayload(event.detail.value as Mode));
    }
}

