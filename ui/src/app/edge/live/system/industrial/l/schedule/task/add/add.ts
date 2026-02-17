import { Component } from "@angular/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { AddTaskComponent } from "src/app/shared/components/schedule/add/add-task.component";
import { TaskFormComponent } from "src/app/shared/components/schedule/form/task-form.component";
import { JsCalendarAddTaskComponent } from "src/app/shared/components/schedule/js-calendar-add-task";
import { TSignalValue } from "src/app/shared/type/utility";

@Component({
    templateUrl: "./add.html",
    imports: [
        AddTaskComponent,
        CommonUiModule,
    ],
})
export class SystemIndustrialLAddTaskComponent extends JsCalendarAddTaskComponent {
    public allowedPeriods: TSignalValue<TaskFormComponent["allowedPeriods"]> = ["monthly"];
}
