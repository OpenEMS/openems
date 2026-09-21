import { ChangeDetectionStrategy, Component, model } from "@angular/core";
import { TZDate } from "@date-fns/tz";
import { filter, take } from "rxjs";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { JsCalendar } from "src/app/shared/components/schedule/js-calendar-task";
import { ScheduleComponent } from "src/app/shared/components/schedule/schedule.component";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";
import { DateTimeFormats, DateTimeUtils } from "src/app/shared/utils/datetime/datetime-utils";
import { ControllerEvseSingleShared } from "../../shared/shared";
import { EvseManualPayload } from "./js-calender-utils";

interface SmartEventPayload {
    class: "Smart";
    sessionEnergyMinimum: number;
}

interface SmartEventViewModel {
    uid: string;
    sessionEnergyMinimum: number;
    endTime: string;
    recurrenceText: string;
}

@Component({
    templateUrl: "./schedule.component.html",
    standalone: true,
    providers: [{ provide: DataService, useClass: LiveDataService }],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [ScheduleComponent, ComponentsBaseModule, CommonUiModule],
})
export class EvseScheduleComponent extends AbstractModal {
    protected readonly CONVERT_TO_MODE_LABEL = ControllerEvseSingleShared.CONVERT_TO_MODE_LABEL(this.translate);
    protected channel: ChannelAddress | null = null;
    protected schedule = model<JsCalendar.ScheduleVM[]>([]);
    protected payload = model(new EvseManualPayload());
    protected smartEvents: SmartEventViewModel[] = [];
    protected canWrite: boolean = this.payload().canWrite(this.edge);

    public override async updateComponent(config: EdgeConfig) {
        return new Promise<void>((res) => {
            this.route.params
                .pipe(
                    filter((params) => params != null),
                    take(1),
                )
                .subscribe((params) => {
                    this.component = config.getComponent(params.componentId);
                    this.channel = new ChannelAddress(params.componentId, "_PropertyMode");
                    res();
                });
        });
    }

    protected readonly manualTaskFilter = (task: JsCalendar.Task): boolean =>
        (task["openems.io:payload"] as Record<string, unknown>)?.["class"] !== "Smart";

    protected updateSmartEvents(tasks: JsCalendar.Task[]): void {
        this.smartEvents = tasks
            .map((task) => this.toSmartEventViewModel(task))
            .filter((event): event is SmartEventViewModel => event !== null);
    }

    private toSmartEventViewModel(task: JsCalendar.Task): SmartEventViewModel | null {
        const payload = task["openems.io:payload"];
        if (!this.isSmartEventPayload(payload) || task.uid == null || task.duration == null) {
            return null;
        }

        const start = /^\d{2}:\d{2}(:\d{2})?$/.test(task.start) ? `1970-01-01T${task.start}` : task.start;
        const end = JsCalendar.Utils.calculateEndTimeFromDuration(start, task.duration);
        if (end == null) {
            return null;
        }

        const endDate = new TZDate(end, DateTimeUtils.getLocaleTimeZone());

        return {
            uid: task.uid,
            sessionEnergyMinimum: payload.sessionEnergyMinimum,
            endTime: DateTimeUtils.format(endDate, DateTimeFormats.HOUR_MINUTE) ?? "",
            recurrenceText: ScheduleComponent.translateRecurrence(task.recurrenceRules ?? [], this.translate),
        };
    }

    private isSmartEventPayload(payload: unknown): payload is SmartEventPayload {
        if (payload == null || typeof payload !== "object") {
            return false;
        }

        const value = payload as Record<string, unknown>;
        return value["class"] === "Smart" && typeof value["sessionEnergyMinimum"] === "number";
    }
}
