import { Component, model } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { SelectCustomEvent } from "@ionic/core";
import { TranslateService } from "@ngx-translate/core";
import { NgxSpinnerModule } from "ngx-spinner";
import { v4 as uuidv4 } from "uuid";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { Language } from "src/app/shared/type/language";
import de from "../../i18n/de.json";
import en from "../../i18n/en.json";
import { JsCalendar } from "../../js-calendar-task";

@Component({
    selector: "oe-schedule-task-form-weekly",
    templateUrl: "./weekly.html",
    imports: [
        CommonUiModule,
        NgxSpinnerModule,
        ReactiveFormsModule,
    ],
})
export class TaskFormWeeklyComponent {

    public recurrenceRuleByDay = model<Extract<JsCalendar.Types.RecurrenceRule, { frequency: "weekly" }> | null>(null);

    protected readonly daySelection = TaskFormWeeklyComponent.WEEK_DAYS(this.translate);
    protected readonly spinnerId: string = uuidv4();

    constructor(private translate: TranslateService) {
        Language.normalizeAdditionalTranslationFiles({ de: de, en: en }).then((translations) => {
            for (const { lang, translation, shouldMerge } of translations) {
                translate.setTranslation(lang, translation, shouldMerge);
            }
        });
    }

    public static readonly WEEK_DAYS = (translate: TranslateService) => ([
        { key: "mo", label: translate.instant("GENERAL.WEEK.MONDAY") },
        { key: "tu", label: translate.instant("GENERAL.WEEK.TUESDAY") },
        { key: "we", label: translate.instant("GENERAL.WEEK.WEDNESDAY") },
        { key: "th", label: translate.instant("GENERAL.WEEK.THURSDAY") },
        { key: "fr", label: translate.instant("GENERAL.WEEK.FRIDAY") },
        { key: "sa", label: translate.instant("GENERAL.WEEK.SATURDAY") },
        { key: "su", label: translate.instant("GENERAL.WEEK.SUNDAY") },
    ]) as const;

    protected setDays(event: SelectCustomEvent<ReturnType<typeof TaskFormWeeklyComponent.WEEK_DAYS>[number]["key"][]>) {
        this.recurrenceRuleByDay.update((el) => {
            if (el == null) {
                return el;
            }
            el.byDay = event.target.value;
            return el;
        });
    }
}
