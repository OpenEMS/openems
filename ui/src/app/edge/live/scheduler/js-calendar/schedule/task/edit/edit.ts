import { ChangeDetectorRef, Component, model } from "@angular/core";
import { FormBuilder } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { EditTaskComponent } from "src/app/shared/components/schedule/edit/edit-task.component";
import { TaskFormComponent } from "src/app/shared/components/schedule/form/task-form.component";
import { JsCalendarEditTaskComponent } from "src/app/shared/components/schedule/js-calendar-edit-task";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { Service, Websocket } from "src/app/shared/shared";
import { Language } from "src/app/shared/type/language";
import { TSignalValue } from "src/app/shared/type/utility";
import de from "../../../i18n/de.json";
import en from "../../../i18n/en.json";
import { SharedSchedulerJsCalendar } from "../../../shared-scheduler-js-calendar";

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
export class SchedulerJsCalendarEditTaskComponent extends JsCalendarEditTaskComponent {
    public payload = model<SharedSchedulerJsCalendar.SchedulerJsCalendarPayload>(new SharedSchedulerJsCalendar.SchedulerJsCalendarPayload());
    protected allowedPeriods: TSignalValue<TaskFormComponent["allowedPeriods"]> = ["daily", "weekly", "monthly"];
    protected controllerOptions: { value: string, label: string }[] = [];

    constructor(
        protected override websocket: Websocket,
        protected override route: ActivatedRoute,
        protected override service: Service,
        public override modalController: ModalController,
        protected override translate: TranslateService,
        public override formBuilder: FormBuilder,
        public override ref: ChangeDetectorRef,
    ) {
        super(websocket, route, service, modalController, translate, formBuilder, ref);
        Language.normalizeAdditionalTranslationFiles({ de: de, en: en }).then((translations) => {
            for (const { lang, translation, shouldMerge } of translations) {
                this.translate.setTranslation(lang, translation, shouldMerge);
            }
        });
    }


    public ionViewWillEnter() {
        this.controllerOptions = SharedSchedulerJsCalendar.getControllerOptions(this.service);
    }

    protected setValue(event: CustomEvent) {
        this.payload.update(el => { el.setValue({ controllerIds: [event.detail.value] as string[] }); return el; });
    }
}
