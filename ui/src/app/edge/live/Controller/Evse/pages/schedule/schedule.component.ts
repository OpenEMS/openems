// @ts-strict-ignore
import { ChangeDetectorRef, Component, Inject } from "@angular/core";
import { FormBuilder, FormControl, FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { filter, take } from "rxjs";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { NavigationService } from "src/app/shared/components/navigation/service/navigation.service";
import { NavigationTree } from "src/app/shared/components/navigation/shared";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { GetAllTasks } from "src/app/shared/jsonrpc/request/getAllTasks";
import { GetAllTasksResponse, SingleTask } from "src/app/shared/jsonrpc/response/getAllTasksResponse";
import { EdgeConfig, Service, Websocket } from "src/app/shared/shared";
import { ControllerEvseSingleShared } from "../../shared/shared";

@Component({
    templateUrl: "./schedule.component.html",
    standalone: false,
    providers: [
        { provide: DataService, useClass: LiveDataService },
    ],
    styles: [`
        ::ng-deep formly-form{
            height: 100% !important;
        }
            .schedule-table {
  --ion-grid-padding: 0;
  font-size: 15px;
}

.header-row {
  font-weight: bold;
  border-bottom: 1px solid #ccc;
  padding-bottom: 6px;
  margin-bottom: 4px;
  text-transform: uppercase;
}

.data-row {
  padding: 8px 0;
  border-bottom: 1px solid rgba(0,0,0,0.08);
}

.edit-col ion-button {
  height: 26px;
  font-size: 12px;
}

.chip-repeat,
.chip-controller {
  margin-right: 4px;
  height: 26px;
  font-size: 12px;
}

.repeat-col ion-chip {
  background: #e8f7e8;
}

.controllers-col ion-chip {
  color: #fff;
  font-weight: bold;
}`,
    ],
})
export class ScheduleComponent extends AbstractModal {
    public static formControlName: string = "schedule";
    protected schedule: ScheduleVM[] = [];
    protected readonly CONVERT_TO_MODE_LABEL = ControllerEvseSingleShared.CONVERT_TO_MODE_LABEL(this.translate);

    constructor(
        @Inject(Websocket) protected override websocket: Websocket,
        @Inject(ActivatedRoute) protected override route: ActivatedRoute,
        @Inject(Service) protected override service: Service,
        @Inject(ModalController) public override modalController: ModalController,
        @Inject(TranslateService) protected override translate: TranslateService,
        @Inject(FormBuilder) public override formBuilder: FormBuilder,
        public override ref: ChangeDetectorRef,
        private navigationService: NavigationService,
    ) {
        super(websocket, route, service, modalController, translate, formBuilder, ref);
    }

    public parseDuration(item: SingleTask): string {
        const value = item.duration;
        if (typeof value !== "string") {
            return "";
        }

        const match =
            /^P(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?)?$/.exec(value);

        if (!match) {
            return "";
        }

        const days = Number(match[1] || 0);
        const hours = Number(match[2] || 0);
        const minutes = Number(match[3] || 0);
        const seconds = Number(match[4] || 0);

        const parts: string[] = [];

        if (days > 0) {
            parts.push(`${days}d`);
        }
        if (hours > 0) {
            parts.push(`${hours}h`);
        }

        if (minutes > 0) {
            parts.push(`${minutes}m`);
        }
        if (seconds > 0) {
            parts.push(`${seconds}s`);
        }

        return parts.length > 0 ? parts.join(" ") : "0s";
    }

    public parsePayload(item: SingleTask): string {
        const value = item["openems.io:payload"];
        if (value !== undefined) {
            return ControllerEvseSingleShared.CONVERT_TO_MODE_LABEL(this.translate)(value.mode);
        } else {
            return "/";
        }
    }

    public parseRecurrence(item: SingleTask): string {
        const value = item.recurrenceRules;
        if (value) {
            const rule = value[0];
            let result = translateRecurrence(rule.frequency, this.translate);
            if (rule.byDay && rule.byDay.length > 0) {
                const days = rule.byDay.map(d => translateDay(d, this.translate)).join(", ");
                result += ` (${days})`;
            }

            if (rule.until) {
                result += ` until ${rule.until}`;
            }

            return result;
        }
        return "/";
    }

    editTaskPage(uid: string) {
        const tree = new NavigationTree(
            "task",
            { baseString: "schedule/task/" + uid },
            { color: "success", name: "play-outline" },
            "task",
            "label",
            [],
            null
        );

        this.navigationService.setChildToCurrentNavigation(tree);
        this.navigationService.navigateTo(tree);
    }


    public override async updateComponent(config: EdgeConfig) {
        return new Promise<void>((res) => {
            this.route.params.pipe(filter(params => params != null), take(1)).subscribe((params) => {
                this.component = config.getComponent(params.componentId);
                res();
            });
        });

    }

    public override onIsInitialized(): void {
        this.edge?.sendRequest(this.websocket, new ComponentJsonApiRequest({
            componentId: this.component.id,
            payload: new GetAllTasks(),
        })
        ).then((resp: GetAllTasksResponse) => {
            this.schedule = resp.result.tasks.map(item => ({
                uid: item.uid,
                start: item.start,
                durationText: this.parseDuration(item),
                recurrenceText: this.parseRecurrence(item),
                payloadText: this.parsePayload(item),
            }));
        });
    }

    protected override getFormGroup(): FormGroup {
        return new FormGroup({
            task: new FormControl(null),
        });
    }
}

function translateDay(d: string, translate: TranslateService): string {
    switch (d) {
        case "mo": return translate.instant("EVSE_SINGLE.SCHEDULE.DAY.MONDAY");
        case "tu": return translate.instant("EVSE_SINGLE.SCHEDULE.DAY.TUESDAY");
        case "we": return translate.instant("EVSE_SINGLE.SCHEDULE.DAY.WEDNESDAY");
        case "th": return translate.instant("EVSE_SINGLE.SCHEDULE.DAY.THURSDAY");
        case "fr": return translate.instant("EVSE_SINGLE.SCHEDULE.DAY.FRIDAY");
        case "sa": return translate.instant("EVSE_SINGLE.SCHEDULE.DAY.SATURDAY");
        case "su": return translate.instant("EVSE_SINGLE.SCHEDULE.DAY.SUNDAY");
    }
}

function translateRecurrence(d: string, translate: TranslateService): string {
    switch (d) {
        case "daily": return translate.instant("EVSE_SINGLE.SCHEDULE.FREQ.DAILY");
        case "weekly": return translate.instant("EVSE_SINGLE.SCHEDULE.FREQ.WEEKLY");
        case "monthly": return translate.instant("EVSE_SINGLE.SCHEDULE.FREQ.MONTHLY");
        case "yearly": return translate.instant("EVSE_SINGLE.SCHEDULE.FREQ.YEARLY");
    }
}

interface ScheduleVM {
    uid: string;
    start: string;
    durationText: string;
    recurrenceText: string;
    payloadText: string;
}
