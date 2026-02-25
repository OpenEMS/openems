import { DatePipe, JsonPipe } from "@angular/common";
import { ChangeDetectorRef, Component, ContentChild, effect, Inject, model, TemplateRef } from "@angular/core";
import { FormBuilder, FormControl, FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { NgxSpinnerModule } from "ngx-spinner";
import { filter, take } from "rxjs";
import { v4 as uuidv4 } from "uuid";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { GetAllTasks } from "src/app/shared/jsonrpc/request/getAllTasks";
import { EdgeConfig, Service, Websocket } from "src/app/shared/shared";
import { CommonUiModule } from "../../common-ui.module";
import { PipeComponentsModule } from "../../pipe/pipe.module";
import { Language } from "../../type/language";
import { AssertionUtils } from "../../utils/assertions/assertions.utils";
import { ComponentsBaseModule } from "../components.module";
import { FlatWidgetButtonComponent } from "../flat/flat-widget-button/flat-widget-button";
import de from "./i18n/de.json";
import en from "./i18n/en.json";
import { JsCalendar } from "./js-calendar-task";

@Component({
    selector: "oe-components-scheduler",
    templateUrl: "./schedule.component.html",
    imports: [
        CommonUiModule,
        PipeComponentsModule,
        ComponentsBaseModule,
        FlatWidgetButtonComponent,
        NgxSpinnerModule,
    ],
    providers: [
        { provide: DataService, useClass: LiveDataService },
        DatePipe,
        JsonPipe,
    ],
})
export class ScheduleComponent extends AbstractModal {
    @ContentChild(TemplateRef) public content!: TemplateRef<any>;
    protected payload = model(new JsCalendar.BaseOpenEMSPayload());
    protected schedule = model<JsCalendar.ScheduleVM[]>([]);
    protected spinnerId: string = uuidv4();
    protected canWrite: boolean = false;

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

        effect(() => {
            const edge = this.service.currentEdge();
            if (edge == null) {
                return;
            }

            const config = edge.getConfigSignal()();
            if (config == null || this.isInitialized == false || this.component == null) {
                return;
            }
            this.onIsInitialized();
        });
    }

    private static translateRecurrence(recurrenceRule: JsCalendar.Task["recurrenceRules"], translate: TranslateService): string {
        return recurrenceRule.map(el => {
            switch (el.frequency) {
                case "daily": return translate.instant("JS_SCHEDULE.FREQ.DAILY");
                case "weekly": return translate.instant("JS_SCHEDULE.FREQ.WEEKLY");
                case "monthly": return translate.instant("JS_SCHEDULE.FREQ.MONTHLY");
                case "yearly": return translate.instant("JS_SCHEDULE.FREQ.YEARLY");
                default: return;
            }
        }).join(",");
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
        AssertionUtils.assertIsDefined(this.component);
        const payload = this.payload();
        this.service.startSpinner(this.spinnerId);
        this.edge?.sendRequest<JsCalendar.GetAllTasksResponse>(this.websocket, new ComponentJsonApiRequest({
            componentId: this.component.id,
            payload: new GetAllTasks(),
        })
        ).then((resp) => {
            this.canWrite = payload.canWrite(this.edge);
            this.schedule.set(resp.result.tasks.map(item => {
                return {
                    uid: item.uid,
                    start: item.start,
                    end: JsCalendar.Utils.calculateEndTimeFromDuration(item?.start ?? null, item?.duration ?? null),
                    durationText: "",
                    recurrenceText: this.parseRecurrence(item),
                    recurrenceRules: item.recurrenceRules,
                    payloadText: payload.toPayloadText(this.translate)(item),
                } as JsCalendar.ScheduleVM;
            }));
        }).finally(() => {
            this.service.stopSpinner(this.spinnerId);
        });
    }

    protected override getFormGroup(): FormGroup {
        return new FormGroup({
            task: new FormControl(null),
        });
    }
    private parseRecurrence(item: JsCalendar.Task): string {
        return ScheduleComponent.translateRecurrence(item.recurrenceRules, this.translate);
    }
}

