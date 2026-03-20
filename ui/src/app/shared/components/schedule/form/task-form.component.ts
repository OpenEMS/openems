import { JsonPipe } from "@angular/common";
import { Component, model, ModelSignal } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { NgxSpinnerModule } from "ngx-spinner";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { Language } from "src/app/shared/type/language";
import de from "../i18n/de.json";
import en from "../i18n/en.json";
import { JsCalendar } from "../js-calendar-task";
import { TaskFormTimeComponent } from "./daily/daily";
import { TaskFormMonthlyComponent } from "./monthly/monthly";

@Component({
    selector: "oe-schedule-task-form",
    templateUrl: "./task-form.component.html",
    imports: [
        CommonUiModule,
        NgxSpinnerModule,
        ReactiveFormsModule,
        TaskFormTimeComponent,
        TaskFormMonthlyComponent,
        JsonPipe,
    ],
})
export class TaskFormComponent {
    public allowedPeriods = model<JsCalendar.Task["recurrenceRules"][number]["frequency"][]>([]);
    public recurrenceRuleByDay = model<JsCalendar.Task["recurrenceRules"][number] | null>(null);

    protected startTime: ModelSignal<string | null> = model<string | null>(null);
    protected endTime: ModelSignal<string | null> = model<string | null>(null);
    protected payload = model<JsCalendar.OpenEMSPayload | null>(null);

    constructor(private translate: TranslateService) {
        Language.normalizeAdditionalTranslationFiles({ de: de, en: en }).then((translations) => {
            for (const { lang, translation, shouldMerge } of translations) {
                translate.setTranslation(lang, translation, shouldMerge);
            }
        });
    }

    protected setRecurrenceRuleByDay(event: CustomEvent) {
        this.recurrenceRuleByDay.update(el => ({ frequency: event.detail.value, byDay: el?.byDay ?? [] }));
    }
}
