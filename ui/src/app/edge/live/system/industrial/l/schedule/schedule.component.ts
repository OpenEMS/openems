import { ChangeDetectorRef, Component, model } from "@angular/core";
import { FormBuilder } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { JsCalendar } from "src/app/shared/components/schedule/js-calendar-task";
import { ScheduleComponent } from "src/app/shared/components/schedule/schedule.component";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { HelpPopoverButtonComponent } from "src/app/shared/components/shared/view-component/help-popover/help-popover";
import { Service, Websocket } from "src/app/shared/shared";
import { Language } from "src/app/shared/type/language";
import de from "../i18n/de.json";
import en from "../i18n/en.json";
import { SharedSystemIndustrialL } from "../shared-system-industrial-l";

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
        HelpPopoverButtonComponent,
    ],
})
export class IndustrialLScheduleComponent extends AbstractModal {

    protected schedule = model<JsCalendar.ScheduleVM[]>([]);
    protected payload = model<SharedSystemIndustrialL.IndustrialLPayload>(new SharedSystemIndustrialL.IndustrialLPayload());

    constructor(protected override websocket: Websocket,
        protected override route: ActivatedRoute,
        protected override service: Service,
        public override modalController: ModalController,
        protected override translate: TranslateService,
        public override formBuilder: FormBuilder,
        public override ref: ChangeDetectorRef) {
        super(websocket, route, service, modalController, translate, formBuilder, ref);
        Language.normalizeAdditionalTranslationFiles({ de: de, en: en }).then((translations) => {
            for (const { lang, translation, shouldMerge } of translations) {
                translate.setTranslation(lang, translation, shouldMerge);
            }
        });
    }
}
