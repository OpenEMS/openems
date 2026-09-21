import { ChangeDetectionStrategy, Component, model } from "@angular/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { AddTaskComponent } from "src/app/shared/components/schedule/add/add-task.component";
import { TaskFormComponent } from "src/app/shared/components/schedule/form/task-form.component";
import { JsCalendarAddTaskComponent } from "src/app/shared/components/schedule/js-calendar-add-task";
import { TSignalValue } from "src/app/shared/type/utility";
import { BaseMode, CONVERT_TO_BASE_MODE_LABEL } from "../../../shared/shared";
import { HeatpumpPayload } from "../../heatpump-payload";

@Component({
    templateUrl: "./add.html",
    standalone: true,
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [AddTaskComponent, CommonUiModule],
})
export class HeatPumpAddTaskComponent extends JsCalendarAddTaskComponent {
    public payload = model<HeatpumpPayload>(new HeatpumpPayload());
    public allowedPeriods = model<TSignalValue<TaskFormComponent["allowedPeriods"]>>(["daily", "weekly", "monthly"]);
    protected baseModeOptions: { value: BaseMode; label: string }[] = Object.values(BaseMode).map((baseMode) => ({
        value: baseMode,
        label: CONVERT_TO_BASE_MODE_LABEL(this.translate)(baseMode),
    }));

    setValue(event: CustomEvent) {
        this.payload.update((el) => {
            el.setValue({ baseMode: event.detail.value });
            return el;
        });
    }
}
