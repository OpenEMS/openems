import { Component, model } from "@angular/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { JsCalendar } from "src/app/shared/components/schedule/js-calendar-task";
import { ScheduleComponent } from "src/app/shared/components/schedule/schedule.component";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { SharedSchedulerJsCalendar } from "../shared-scheduler-js-calendar";

@Component({
    templateUrl: "./schedule.component.html",
    standalone: true,
    providers: [
        { provide: DataService, useClass: LiveDataService },
    ],
    imports: [
        ScheduleComponent,
        ComponentsBaseModule,
        CommonUiModule,
    ],
})
export class ScheduleJsCalendarComponent extends AbstractModal {

    protected schedule = model<JsCalendar.ScheduleVM[]>([]);
    protected payload = model<SharedSchedulerJsCalendar.SchedulerJsCalendarPayload>(new SharedSchedulerJsCalendar.SchedulerJsCalendarPayload());
}
