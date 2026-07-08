import { Component, model } from "@angular/core";
import { Mode } from "src/app/edge/live/Controller/Heat/settings/settings";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { EditTaskComponent } from "src/app/shared/components/schedule/edit/edit-task.component";
import { TaskFormComponent } from "src/app/shared/components/schedule/form/task-form.component";
import { JsCalendarEditTaskComponent } from "src/app/shared/components/schedule/js-calendar-edit-task";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { TSignalValue } from "src/app/shared/type/utility";
import { CONVERT_TO_MODE_LABEL } from "../../../shared/shared";
import { HeatManualPayload } from "../../js-calendar-utils";

@Component({
    templateUrl: "./edit.html",
    standalone: true,
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
export class HeatEditTaskComponent extends JsCalendarEditTaskComponent {

    public allowedPeriods = model<TSignalValue<TaskFormComponent["allowedPeriods"]>>(["daily", "weekly", "monthly"]);
    public payload = model<HeatManualPayload>(new HeatManualPayload());
    protected modeOptions: { value: Mode, label: string }[] = Object.values(Mode).map(mode => ({
        value: mode,
        label: CONVERT_TO_MODE_LABEL(this.translate)(mode),
    }));
    protected isAskomaReadOnly: boolean = false;

    setValue(event: CustomEvent) {
        this.payload.update(el => { el.setValue({ mode: event.detail.value }); return el; });
    }

    protected override updateComponent(): void {
        this.isAskomaReadOnly = this.component?.factoryId === "Heat.Askoma" && this.component.properties?.readOnly === true;
    }
}
