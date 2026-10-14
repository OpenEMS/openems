import { ChangeDetectionStrategy, Component, model, ModelSignal } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { NgxSpinnerModule } from "ngx-spinner";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { Language } from "src/app/shared/type/language";
import cz from "../i18n/cz.json";
import de from "../i18n/de.json";
import en from "../i18n/en.json";
import es from "../i18n/es.json";
import fr from "../i18n/fr.json";
import ja from "../i18n/ja.json";
import nl from "../i18n/nl.json";
import { JsCalendar } from "../js-calendar-task";
import { TaskFormTimeComponent } from "./daily/daily";
import { TaskFormMonthlyComponent } from "./monthly/monthly";
import { TaskFormWeeklyComponent } from "./weekly/weekly";

@Component({
    selector: "oe-schedule-task-form",
    templateUrl: "./task-form.component.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [
        CommonUiModule,
        NgxSpinnerModule,
        ReactiveFormsModule,
        TaskFormTimeComponent,
        TaskFormMonthlyComponent,
        TaskFormWeeklyComponent,
    ],
})
export class TaskFormComponent {
    public allowedPeriods = model<JsCalendar.Task["recurrenceRules"][number]["frequency"][]>([]);
    public recurrenceRuleByDay = model<JsCalendar.Task["recurrenceRules"][number] | null>(null);

    protected startTime: ModelSignal<string | null> = model<string | null>(null);
    protected endTime: ModelSignal<string | null> = model<string | null>(null);
    protected payload = model<JsCalendar.OpenEMSPayload | null>(null);

    constructor(private translate: TranslateService) {
        Language.normalizeAdditionalTranslationFiles({ cs: cz, de: de, en: en, es: es, fr: fr, ja: ja, nl: nl }).then(
            (translations) => {
                for (const { lang, translation, shouldMerge } of translations) {
                    translate.setTranslation(lang, translation, shouldMerge);
                }
            },
        );
    }

    protected setRecurrenceRuleByDay(event: CustomEvent) {
        this.recurrenceRuleByDay.update((_el) => {
            return { frequency: event.detail.value };
        });
    }
}
