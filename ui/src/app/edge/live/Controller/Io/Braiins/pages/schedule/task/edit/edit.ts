// @ts-strict-ignore
import { Component, model, ChangeDetectionStrategy } from "@angular/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { EditTaskComponent } from "src/app/shared/components/schedule/edit/edit-task.component";
import { TaskFormComponent } from "src/app/shared/components/schedule/form/task-form.component";
import { JsCalendarEditTaskComponent } from "src/app/shared/components/schedule/js-calendar-edit-task";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { TSignalValue } from "src/app/shared/type/utility";
import { ControllerBraiinsShared } from "../../../../shared/shared";
import { ControllerBraiinsManualPayload } from "../../js-calender-utils";

type PayloadMode = Parameters<ControllerBraiinsManualPayload["setValue"]>[0]["mode"];

type ModeOption = {
    value: PayloadMode;
    label: string;
};

type ModeChangeEvent = CustomEvent<{
    value: PayloadMode;
}>;

@Component({
    templateUrl: "./edit.html",
    imports: [CommonUiModule, EditTaskComponent, ComponentsBaseModule],
    providers: [{ provide: DataService, useClass: LiveDataService }],
    changeDetection: ChangeDetectionStrategy.Eager,
    styles: [
        `
            ::ng-deep formly-form {
                height: 100% !important;
            }
        `,
    ],
})
export class ControllerBraiinsEditTaskComponent extends JsCalendarEditTaskComponent {
    public allowedPeriods = model<TSignalValue<TaskFormComponent["allowedPeriods"]>>(["daily", "weekly", "monthly"]);
    public payload = model<ControllerBraiinsManualPayload>(new ControllerBraiinsManualPayload());
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
