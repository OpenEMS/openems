import { Component, model, ChangeDetectionStrategy } from "@angular/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { AddTaskComponent } from "src/app/shared/components/schedule/add/add-task.component";
import { TaskFormComponent } from "src/app/shared/components/schedule/form/task-form.component";
import { JsCalendarAddTaskComponent } from "src/app/shared/components/schedule/js-calendar-add-task";
import { TSignalValue } from "src/app/shared/type/utility";
import { CONVERT_TO_MODE_LABEL, PropertyMode } from "../../../shared/shared";
import { HeatManualPayload } from "../../js-calendar-utils";

@Component({
    templateUrl: "./add.html",
    standalone: true,
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [AddTaskComponent, CommonUiModule],
})
export class HeatAddTaskComponent extends JsCalendarAddTaskComponent {
    public payload = model<HeatManualPayload>(new HeatManualPayload());
    public allowedPeriods = model<TSignalValue<TaskFormComponent["allowedPeriods"]>>(["daily", "weekly", "monthly"]);
    protected modeOptions: { value: PropertyMode; label: string }[] = Object.values(PropertyMode).map((mode) => ({
        value: mode,
        label: CONVERT_TO_MODE_LABEL(this.translate)(mode),
    }));
    protected isAskomaReadOnly: boolean = false;

    setValue(event: CustomEvent) {
        this.payload.update((el) => {
            el.setValue({ mode: event.detail.value });
            return el;
        });
    }

    protected override updateComponent(): void {
        this.isAskomaReadOnly =
            this.component?.factoryId === "Heat.Askoma" && this.component.properties?.readOnly === true;
    }
}
