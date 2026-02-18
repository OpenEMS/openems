import { Component, inject, model } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { NgxSpinnerModule } from "ngx-spinner";
import { v4 as uuidv4 } from "uuid";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { Service } from "src/app/shared/shared";
import { Language } from "src/app/shared/type/language";
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

    public recurrenceRuleByDay = model<JsCalendar.Task["recurrenceRules"][number] | null>(null);

    protected readonly nthOfPeriodSelection: number[] = [1, 2, 3, 4];
    protected readonly daySelection = TaskFormMonthlyComponent.WEEK_DAYS(this.translate);
    protected readonly spinnerId: string = uuidv4();
    private service: Service = inject(Service);

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
        { key: "thu", label: translate.instant("GENERAL.WEEK.THURSDAY") },
        { key: "fr", label: translate.instant("GENERAL.WEEK.FRIDAY") },
        { key: "sa", label: translate.instant("GENERAL.WEEK.SATURDAY") },
        { key: "su", label: translate.instant("GENERAL.WEEK.SUNDAY") },
    ]) as const;

    protected add(): void {
        this.recurrenceRuleByDay.update(el => {
            if (el?.byDay) {
                el.byDay?.push({ day: null, nthOfPeriod: null });
            }
            return el;
        });
    }

    protected remove(i: number): void {
        this.recurrenceRuleByDay.update(el => {
            if (el?.byDay) {
                el.byDay = el.byDay?.filter((_el, index) => index !== i) ?? [];
            }
            return el;
        });
    }

    protected setDay(item: NonNullable<JsCalendar.Task["recurrenceRules"][number]["byDay"]>[number], event: CustomEvent) {
        item.day = event.detail.value;
    }

    protected setNthOfPeriod(item: NonNullable<JsCalendar.Task["recurrenceRules"][number]["byDay"]>[number], event: CustomEvent) {
        item.nthOfPeriod = event.detail.value;
    }
}

export type WeekDayKeys = ReturnType<typeof TaskFormMonthlyComponent.WEEK_DAYS>[number]["key"];
