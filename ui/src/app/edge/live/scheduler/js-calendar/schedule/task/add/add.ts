import { ChangeDetectorRef, Component, model } from "@angular/core";
import { FormBuilder } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { AddTaskComponent } from "src/app/shared/components/schedule/add/add-task.component";
import { TaskFormComponent } from "src/app/shared/components/schedule/form/task-form.component";
import { JsCalendarAddTaskComponent } from "src/app/shared/components/schedule/js-calendar-add-task";
import { Service, Websocket } from "src/app/shared/shared";
import { Language } from "src/app/shared/type/language";
import { TSignalValue } from "src/app/shared/type/utility";
import de from "../../../i18n/de.json";
import en from "../../../i18n/en.json";
import { SharedSchedulerJsCalendar } from "../../../shared-scheduler-js-calendar";

@Component({
    templateUrl: "./add.html",
    imports: [
        AddTaskComponent,
        CommonUiModule,
    ],
})
export class SchedulerJsCalendarAddTaskComponent extends JsCalendarAddTaskComponent {
    public allowedPeriods: TSignalValue<TaskFormComponent["allowedPeriods"]> = ["daily", "weekly", "monthly"];
    protected payload = model<SharedSchedulerJsCalendar.SchedulerJsCalendarPayload>(new SharedSchedulerJsCalendar.SchedulerJsCalendarPayload());
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

    setValue(event: CustomEvent) {
        this.payload.update(el => { el.setValue({ controllerIds: [event.detail.value] as string[] }); return el; });
    }

    public override async ionViewWillEnter(): Promise<void> {
        await super.ionViewWillEnter();
        this.controllerOptions = SharedSchedulerJsCalendar.getControllerOptions(this.service);
    }
}
