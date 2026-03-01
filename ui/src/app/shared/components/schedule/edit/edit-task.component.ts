import { Location } from "@angular/common";
import { ChangeDetectorRef, Component, Inject, inject, model } from "@angular/core";
import { FormBuilder } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { v4 as uuidv4 } from "uuid";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { validateTaskInputs } from "src/app/shared/components/schedule/form/task-form-validation";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { JsonRpcUtils } from "src/app/shared/jsonrpc/jsonrpcutils";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { DeleteTask } from "src/app/shared/jsonrpc/request/deleteTaskRequest";
import { GetAllTasks } from "src/app/shared/jsonrpc/request/getAllTasks";
import { Service, Websocket } from "src/app/shared/shared";
import { Language } from "src/app/shared/type/language";
import { TSignalValue } from "src/app/shared/type/utility";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { DateUtils } from "src/app/shared/utils/date/dateutils";
import { EdgeConfig } from "../../edge/edgeconfig";
import { TaskFormComponent } from "../form/task-form.component";
import de from "../i18n/de.json";
import en from "../i18n/en.json";
import { JsCalendarEditTaskComponent } from "../js-calendar-edit-task";
import { JsCalendar } from "../js-calendar-task";

@Component({
    selector: "oe-components-scheduler-edit-task",
    templateUrl: "./edit-task.component.html",
    imports: [
        CommonUiModule,
        ComponentsBaseModule,
        TaskFormComponent,
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

export class EditTaskComponent extends JsCalendarEditTaskComponent {
    public payload = model<JsCalendar.OpenEMSPayload<string> | null>(null);
    public allowedPeriods = model<TSignalValue<TaskFormComponent["allowedPeriods"]>>([]);
    public recurrenceRuleByDay = model<JsCalendar.UpdateTask["recurrenceRules"][number] | null>(null);
    public startTime = model<string | null>(null);
    public endTime = model<string | null>(null);

    protected readonly spinnerId: string = uuidv4();
    protected componentId: string | null = null;
    protected uid: string | null = null;

    private location: Location = inject(Location);

    constructor(
        @Inject(Websocket) protected override websocket: Websocket,
        @Inject(ActivatedRoute) protected override route: ActivatedRoute,
        @Inject(Service) protected override service: Service,
        @Inject(ModalController) public override modalController: ModalController,
        @Inject(TranslateService) protected override translate: TranslateService,
        @Inject(FormBuilder) public override formBuilder: FormBuilder,
        public override ref: ChangeDetectorRef,
    ) {
        super(websocket, route, service, modalController, translate, formBuilder, ref);
        Language.normalizeAdditionalTranslationFiles({ de: de, en: en }).then((translations) => {
            for (const { lang, translation, shouldMerge } of translations) {
                translate.setTranslation(lang, translation, shouldMerge);
            }
        });
    }

    public override async onIsInitialized(): Promise<void> {
        const url = this.routeService.currentUrl();
        if (url === null) {
            return;
        }

        const componentId = this.routeService.getRouteParam<string>("componentId");
        const uid = this.routeService.getRouteParam<string>("taskId");

        this.componentId = componentId;
        this.uid = uid;

        if (this.edge === null) {
            return;
        }

        AssertionUtils.assertIsDefined(componentId);

        const [err, _result] = await JsonRpcUtils.handle(
            this.edge.sendRequest(this.websocket, new ComponentJsonApiRequest({
                componentId: componentId,
                payload: new GetAllTasks(),
            }))
        );

        if (err) {
            console.error("Error fetching tasks:", err);
            return;
        }

        const result = _result as JsCalendar.GetAllTasksResponse;
        const task = result.result.tasks.find(t => t.uid === this.uid);

        if (task === null || task === undefined) {
            console.warn("Task with the given UID not found.");
            return;
        }


        this.startTime.set(task.start);
        this.endTime.set(JsCalendar.Utils.calculateEndTimeFromDuration(task?.start ?? null, task?.duration ?? null));
        this.payload.update(el => el?.update(el, task) ?? null);
        this.recurrenceRuleByDay.update(el => task.recurrenceRules?.[0]);
    }

    protected override updateComponent(config: EdgeConfig): void {
        const componentId = this.routeService.getRouteParam<string>("componentId");
        if (componentId == null) {
            return;
        }
        this.component = config.getComponent(componentId);
    }

    protected async saveTask(): Promise<void> {
        const startTime = this.startTime();
        const endTime = this.endTime();
        const payload = this.payload();
        if (this.validateInputs() == false) {
            return;
        }

        if (startTime === null || endTime === null || payload === null) {
            return;
        }

        const startDate = DateUtils.stringToDate(startTime);
        const endDate = DateUtils.stringToDate(endTime);

        if (DateUtils.isDateBefore(endDate, startDate)) {
            this.service.toast(this.translate.instant("JS_SCHEDULE.VALIDATION_ERROR_6"), "danger");
            return;
        }

        const [start, duration] = [JsCalendar.Utils.formatIsoLocalDateTime(startDate), JsCalendar.Utils.computeIsoDuration(startDate, endDate)];
        if (start == null || duration == null) {
            this.service.toast(this.translate.instant("JS_SCHEDULE.VALIDATION_ERROR_7"), "danger");
            return;
        }

        const recurrenceRuleByDay = this.recurrenceRuleByDay();
        const task: JsCalendar.UpdateTask = {
            "@type": "Task",
            "uid": this.uid ?? "",
            "start": start,
            "recurrenceRules": recurrenceRuleByDay != null ? [recurrenceRuleByDay] : [],
            ...duration,
            ...this.payload()?.toOpenEMSPayload(),
        };

        const fullUrl = this.routeService?.getCurrentUrl() ?? null;
        if (fullUrl === null) {
            return;
        }

        const componentId = this.routeService.getRouteParam<string>("componentId");

        if (this.edge === null || componentId === null) {
            return;
        }

        const [err] = await JsonRpcUtils.handle(
            this.edge.sendRequest(this.websocket, new ComponentJsonApiRequest({
                componentId,
                payload: new JsCalendar.UpdateTaskRequest({ task }),
            }))
        );

        if (err) {
            this.service.toast(this.translate.instant("JS_SCHEDULE.EDIT_ERROR"), "danger");
            return;
        }

        this.service.toast(this.translate.instant("JS_SCHEDULE.EDIT_SUCCESS"), "success");
        this.location.back();
    }

    protected async deleteTask(): Promise<void> {
        if (this.edge === null) {
            return;
        }

        if (this.componentId === null || this.uid === null) {
            return;
        }

        const [err] = await JsonRpcUtils.handle(
            this.edge.sendRequest(this.websocket, new ComponentJsonApiRequest({
                componentId: this.componentId,
                payload: new DeleteTask.Request({ uid: this.uid }),
            }))
        );

        if (err) {
            this.service.toast(this.translate.instant("JS_SCHEDULE.DELETE_ERROR"), "danger");
            return;
        }

        this.service.toast(this.translate.instant("JS_SCHEDULE.DELETE_SUCCESS"), "success");
        this.location.back();
    }

    private validateInputs(): boolean {
        const result = validateTaskInputs(
            this.startTime(),
            this.endTime(),
            this.payload(),
            this.recurrenceRuleByDay(),
            this.translate
        );

        if (!result.valid) {
            this.service.toast(result.message!, "danger");
            return false;
        }

        return true;
    }
}
