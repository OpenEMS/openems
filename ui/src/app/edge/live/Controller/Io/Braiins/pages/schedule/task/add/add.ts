import { Component, model, ChangeDetectionStrategy } from "@angular/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { AddTaskComponent } from "src/app/shared/components/schedule/add/add-task.component";
import { TaskFormComponent } from "src/app/shared/components/schedule/form/task-form.component";
import { JsCalendarAddTaskComponent } from "src/app/shared/components/schedule/js-calendar-add-task";
import { TSignalValue } from "src/app/shared/type/utility";
import { ControllerBraiinsShared } from "../../../../shared/shared";
import { ControllerBraiinsManualPayload } from "../../js-calender-utils";

type PayloadMode = NonNullable<Parameters<ControllerBraiinsManualPayload["setValue"]>[0]>["mode"];

type ModeOption = {
    value: PayloadMode;
    label: string;
};

type ModeChangeEvent = CustomEvent<{
    value: PayloadMode;
}>;

@Component({
    templateUrl: "./add.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [AddTaskComponent, CommonUiModule],
})
export class ControllerBraiinsAddTaskComponent extends JsCalendarAddTaskComponent {
    public payload = model<ControllerBraiinsManualPayload>(new ControllerBraiinsManualPayload());
    public allowedPeriods = model<TSignalValue<TaskFormComponent["allowedPeriods"]>>(["daily", "weekly", "monthly"]);
    protected modeOptions: ModeOption[] = Object.values(ControllerBraiinsShared.Mode).map((mode) => ({
        value: mode,
        label: ControllerBraiinsShared.CONVERT_TO_MODE_LABEL(this.translate)(mode),
    }));

    public setValue(event: ModeChangeEvent): void {
        this.payload.update((payload) => {
            payload.setValue({
                class: "Manual",
                mode: event.detail.value,
            });
            return payload;
        });
    }
}
