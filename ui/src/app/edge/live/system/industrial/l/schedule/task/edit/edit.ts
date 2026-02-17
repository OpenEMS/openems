// @ts-strict-ignore
import { Component } from "@angular/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { EditTaskComponent } from "src/app/shared/components/schedule/edit/edit-task.component";
import { TaskFormComponent } from "src/app/shared/components/schedule/form/task-form.component";
import { JsCalendarEditTaskComponent } from "src/app/shared/components/schedule/js-calendar-edit-task";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { TSignalValue } from "src/app/shared/type/utility";

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
export class SystemIndustrialLEditTaskComponent extends JsCalendarEditTaskComponent {
    protected allowedPeriods: TSignalValue<TaskFormComponent["allowedPeriods"]> = ["monthly"];
}
