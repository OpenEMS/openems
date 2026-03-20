// @ts-strict-ignore
import { Component, model } from "@angular/core";
import { Mode } from "src/app/edge/live/Controller/Evse/pages/chargemode/chargemode";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { EditTaskComponent } from "src/app/shared/components/schedule/edit/edit-task.component";
import { TaskFormComponent } from "src/app/shared/components/schedule/form/task-form.component";
import { JsCalendarEditTaskComponent } from "src/app/shared/components/schedule/js-calendar-edit-task";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { TSignalValue } from "src/app/shared/type/utility";
import { ControllerEvseSingleShared } from "../../../../shared/shared";
import { EvseManualPayload } from "../../js-calender-utils";

@Component({
    templateUrl: "./edit.html",
    imports: [
        CommonUiModule,
        EditTaskComponent,
        ComponentsBaseModule,
    ],
    providers: [
        { provide: DataService, useClass: LiveDataService },
    ],
    styles: [`
        ::ng-deep formly-form{
            height: 100% !important;
        }`,
    ],
})
export class EvseEditTaskComponent extends JsCalendarEditTaskComponent {

    public allowedPeriods = model<TSignalValue<TaskFormComponent["allowedPeriods"]>>(["daily", "monthly"]);
    public startTime = model<string | null>(null);
    public endTime = model<string | null>(null);
    public payload = model<EvseManualPayload>(new EvseManualPayload());

    protected formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";
    protected uid: string | null = null;
    protected modeOptions: { value: Mode, label: string }[] = Object.values(Mode).map(mode => ({
        value: mode,
        label: ControllerEvseSingleShared.CONVERT_TO_MODE_LABEL(this.translate)(mode),
    }));

    setValue(event: CustomEvent) {
        this.payload.set(new EvseManualPayload(event.detail.value as Mode));
    }
}
