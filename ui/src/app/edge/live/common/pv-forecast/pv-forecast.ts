import { ChangeDetectionStrategy, ChangeDetectorRef, Component, computed, effect, inject, OnDestroy, } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { TranslateService } from "@ngx-translate/core";
import { addDays, addHours, startOfToday } from "date-fns";

import { CommonUiModule } from "src/app/shared/common-ui.module";
import { GetSchedule } from "src/app/shared/components/edge/config-components/energy/getSchedule";
import { Service, Websocket } from "src/app/shared/shared";
import { Language } from "src/app/shared/type/language";
import de from "./i18n/de.json";
import en from "./i18n/en.json";

/**
 * Production forecast of today and tomorrow (kWh), from the energy schedule of the edge. Tomorrow is shown once the
 * schedule covers its daylight hours.
 */
@Component({
    selector: "oe-pv-forecast",
    templateUrl: "./pv-forecast.html",
    styleUrls: ["./pv-forecast.scss"],
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [CommonUiModule],
})
export class PvForecastComponent implements OnDestroy {
    private static readonly REFRESH_MS = 15 * 60 * 1000;
    /** Tomorrow counts as covered, when the schedule reaches this hour of tomorrow */
    private static readonly COVERED_UNTIL_HOUR = 20;

    protected today: { total: number; remaining: number } | null = null;
    protected tomorrow: number | null = null;
    /** Schedule loaded, but not yet covering tomorrow */
    protected tomorrowPending: boolean = false;

    private readonly service = inject(Service);
    private readonly websocket = inject(Websocket);
    private readonly translate = inject(TranslateService);
    private readonly cdRef = inject(ChangeDetectorRef);
    private readonly edge = computed(() => this.service.currentEdge());
    private timer: ReturnType<typeof setInterval> | null = null;

    constructor() {
        // registered again on language change and on every load: the app's own translation files
        // may be loaded after this component was created and would otherwise replace these texts
        this.registerTranslations();
        this.translate.onLangChange.pipe(takeUntilDestroyed()).subscribe(() => this.registerTranslations());
        effect(() => {
            const edge = this.edge();
            if (this.timer != null) {
                clearInterval(this.timer);
                this.timer = null;
            }
            if (edge == null) {
                return;
            }
            this.load();
            this.timer = setInterval(() => this.load(), PvForecastComponent.REFRESH_MS);
        });
    }

    /** Tomorrow compared to today: 1 more, -1 less, 0 about the same */
    protected get trend(): number {
        if (this.today == null || this.tomorrow == null) {
            return 0;
        }
        const diff = this.tomorrow - this.today.total;
        return Math.abs(diff) < Math.max(0.5, this.today.total * 0.1) ? 0 : Math.sign(diff);
    }

    public ngOnDestroy(): void {
        if (this.timer != null) {
            clearInterval(this.timer);
        }
    }

    protected format(kwh: number): string {
        return (
            new Intl.NumberFormat(Language.getCurrentLanguage().i18nLocaleKey, {
                maximumFractionDigits: kwh < 10 ? 1 : 0,
            }).format(kwh) + " kWh"
        );
    }

    private load(): Promise<void> {
        const edge = this.edge();
        if (edge == null) {
            return Promise.resolve();
        }
        this.registerTranslations();
        // a 'from' other than midnight returns two full days (today and tomorrow), midnight only one
        return GetSchedule.getSchedule(edge, this.websocket, addHours(startOfToday(), 12))
            .then((schedule) => {
                const withProduction = schedule.data.filter((e) => e._sum.ProductionActivePower != null);
                if (withProduction.length === 0) {
                    this.clear();
                    return;
                }
                const today = schedule.calculateEnergyFromPower("today", "ProductionActivePower");
                this.today = { total: today.total, remaining: today.prediction };

                const coveredUntil = addDays(startOfToday(), 1);
                coveredUntil.setHours(PvForecastComponent.COVERED_UNTIL_HOUR);
                const last = withProduction[withProduction.length - 1].timestamp;
                if (last >= coveredUntil) {
                    this.tomorrow = schedule.calculateEnergyFromPower("tomorrow", "ProductionActivePower").total;
                    this.tomorrowPending = false;
                } else {
                    this.tomorrow = null;
                    this.tomorrowPending = true;
                }
                this.cdRef.markForCheck();
            })
            .catch(() => {
                // no energy schedule on this edge: nothing to show
                this.clear();
            });
    }

    private registerTranslations(): void {
        Language.normalizeAdditionalTranslationFiles({ de: de, en: en }).then((translations) => {
            for (const { lang, translation, shouldMerge } of translations) {
                this.translate.setTranslation(lang, translation, shouldMerge);
            }
            this.cdRef.markForCheck();
        });
    }

    private clear(): void {
        this.today = null;
        this.tomorrow = null;
        this.tomorrowPending = false;
        this.cdRef.markForCheck();
    }
}
