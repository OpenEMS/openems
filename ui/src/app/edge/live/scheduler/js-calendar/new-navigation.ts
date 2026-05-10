import { Component, model } from "@angular/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { OneTasksComponent } from "src/app/shared/components/schedule/one-tasks/one-tasks";
import { SharedSchedulerJsCalendar } from "./shared-scheduler-js-calendar";


@Component({
    selector: "oe-controller-evse-single-home",
    templateUrl: "./new-navigation.html",
    standalone: true,
    imports: [
        CommonUiModule,
        ComponentsBaseModule,
        OneTasksComponent,
    ],
})
export class SchedulerJsCalendarComponent extends AbstractModal {

    public payload = model(new SharedSchedulerJsCalendar.SchedulerJsCalendarPayload());
}
