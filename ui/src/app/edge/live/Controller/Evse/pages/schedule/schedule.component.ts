import { Component, model } from "@angular/core";
import { filter, take } from "rxjs";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { JsCalendar } from "src/app/shared/components/schedule/js-calendar-task";
import { ScheduleComponent } from "src/app/shared/components/schedule/schedule.component";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";
import { ControllerEvseSingleShared } from "../../shared/shared";
import { EvseManualPayload } from "./js-calender-utils";

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
export class EvseScheduleComponent extends AbstractModal {

    protected readonly CONVERT_TO_MODE_LABEL = ControllerEvseSingleShared.CONVERT_TO_MODE_LABEL(this.translate);
    protected channel: ChannelAddress | null = null;
    protected schedule = model<JsCalendar.ScheduleVM[]>([]);
    protected payload = model(new EvseManualPayload());

    public override async updateComponent(config: EdgeConfig) {
        return new Promise<void>((res) => {
            this.route.params.pipe(filter(params => params != null), take(1)).subscribe((params) => {
                this.component = config.getComponent(params.componentId);
                this.channel = new ChannelAddress(params.componentId, "_PropertyMode");
                res();
            });
        });
    }
}
