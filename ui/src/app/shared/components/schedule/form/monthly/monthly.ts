import { Component, model } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { NgxSpinnerModule } from "ngx-spinner";
import { v4 as uuidv4 } from "uuid";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { Language } from "src/app/shared/type/language";
import { TIntRange } from "src/app/shared/type/utility";
import de from "../../i18n/de.json";
import en from "../../i18n/en.json";
import { JsCalendar } from "../../js-calendar-task";

@Component({
    selector: "oe-schedule-task-form-monthly",
    templateUrl: "./monthly.html",
    imports: [
        CommonUiModule,
        NgxSpinnerModule,
        ReactiveFormsModule,
    ],
})
export class TaskFormMonthlyComponent {

    public recurrenceRuleByDay = model<Extract<JsCalendar.Types.RecurrenceRule, { frequency: "monthly", byDay: { day: JsCalendar.Types.WeekDayKeys | null, nthOfPeriod: TIntRange<1, 5> | null }[] }> | null>(null);

    protected readonly nthOfPeriodSelection: number[] = [1, 2, 3, 4];
    protected readonly daySelection = TaskFormMonthlyComponent.WEEK_DAYS(this.translate);
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

    protected add(): void {
        this.recurrenceRuleByDay.update((el) => {
            if (el !== null) {
                el.byDay ??= [];
                el.byDay?.push({ day: null, nthOfPeriod: null });
            }
            return el;
        });
    }

    protected remove(i: number): void {
        this.recurrenceRuleByDay.update((el) => {
            if (el !== null) {
                el.byDay ??= [];
                el.byDay = el.byDay?.filter((_el, index) => index !== i) ?? [];
            }
            return el;
        });
    }

    protected setDay(item: MonthlyByDay, event: CustomEvent) {
        item.day = event.detail.value;
    }

    protected setNthOfPeriod(item: MonthlyByDay, event: CustomEvent) {
        item.nthOfPeriod = event.detail.value;
    }
}

type Monthly = JsCalendar.Types.RuleOf<"monthly">;
type MonthlyByDay = Monthly["byDay"][number];
