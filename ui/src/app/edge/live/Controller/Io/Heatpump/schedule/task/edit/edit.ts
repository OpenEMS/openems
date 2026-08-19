import { ChangeDetectionStrategy, Component, model } from "@angular/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { EditTaskComponent } from "src/app/shared/components/schedule/edit/edit-task.component";
import { TaskFormComponent } from "src/app/shared/components/schedule/form/task-form.component";
import { JsCalendarEditTaskComponent } from "src/app/shared/components/schedule/js-calendar-edit-task";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { TSignalValue } from "src/app/shared/type/utility";
import { BaseMode, CONVERT_TO_BASE_MODE_LABEL } from "../../../shared/shared";
import { HeatpumpPayload } from "../../heatpump-payload";

@Component({
    templateUrl: "./edit.html",
    standalone: true,
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [CommonUiModule, EditTaskComponent, ComponentsBaseModule],
    providers: [{ provide: DataService, useClass: LiveDataService }],
    styles: [
        `
            ::ng-deep formly-form {
                height: 100% !important;
            }
        `,
    ],
})
export class HeatPumpEditTaskComponent extends JsCalendarEditTaskComponent {
    public payload = model<HeatpumpPayload>(new HeatpumpPayload());
    public allowedPeriods = model<TSignalValue<TaskFormComponent["allowedPeriods"]>>(["daily", "weekly", "monthly"]);
    protected baseModeOptions: { value: BaseMode; label: string }[] = Object.values(BaseMode).map((mode) => ({
        value: mode,
        label: CONVERT_TO_BASE_MODE_LABEL(this.translate)(mode),
    }));

    setValue(event: CustomEvent) {
        this.payload.update((el) => {
            el.setValue({ baseMode: event.detail.value });
            return el;
        });
    }
}
