import { CommonModule } from "@angular/common";
import { Component, computed, inject, Signal } from "@angular/core";
import { toObservable, toSignal } from "@angular/core/rxjs-interop";
import { ActivatedRoute, Router } from "@angular/router";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { formatRelative } from "date-fns";
import { catchError, from, of, switchMap } from "rxjs";
import { EnergySchedulerV2 } from "src/app/shared/components/edge/config-components/energy/energy";
import { GetSchedule } from "src/app/shared/components/edge/config-components/energy/getSchedule";
import { Converter } from "src/app/shared/components/shared/converter";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { Currency, Edge, EdgeConfig, Service } from "src/app/shared/shared";
import { Language } from "src/app/shared/type/language";
import { LiveDataService } from "../livedataservice";
import de from "./i18n/de.json";
import en from "./i18n/en.json";

interface FenaMessage {
    text: string;
    actions?: { title: string; callback: () => void }[];
}

@Component({
    selector: "oe-fena",
    standalone: true,
    templateUrl: "./fena.component.html",
    imports: [CommonModule, IonicModule, TranslateModule, FormlyModule],
    styleUrl: "./fena.component.scss",
    providers: [{ provide: DataService, useClass: LiveDataService }],
})
export class FenaComponent {
    /**
     * Tracks both the selected edge and its config.
     */
    protected readonly edgeAndConfig = computed(() => {
        const edge = this.service.currentEdge();

        return {
            edge,
            config: edge?.getConfigSignal()() ?? null,
        };
    });

    protected readonly messages: Signal<FenaMessage[]> = toSignal(
        toObservable(this.edgeAndConfig).pipe(
            switchMap(({ edge, config }) => {
                if (edge == null || config == null) {
                    return of<FenaMessage[]>([]);
                }

                if (edge.id !== "fems888") {
                    return of<FenaMessage[]>([]);
                }

                return from(this.fetchSchedulerMessages(edge, config)).pipe(
                    catchError((error) => {
                        console.warn("Error fetching scheduler messages:", error);
                        return of<FenaMessage[]>([]);
                    }),
                );
            }),
        ),
        {
            initialValue: [],
        },
    );

    private readonly dataService = inject(DataService);
    private readonly service = inject(Service);
    private readonly translate = inject(TranslateService);
    private readonly router = inject(Router);
    private readonly route = inject(ActivatedRoute);

    constructor() {
        Language.normalizeAdditionalTranslationFiles({ de: de, en: en }).then((translations) => {
            for (const { lang, translation, shouldMerge } of translations) {
                this.translate.setTranslation(lang, translation, shouldMerge);
            }
        });
    }

    private async fetchSchedulerMessages(edge: Edge, config: EdgeConfig): Promise<FenaMessage[]> {
        const messages: (FenaMessage | null)[] = [];

        const energyScheduler = new EnergySchedulerV2(config);
        await energyScheduler.updateSchedule(edge, this.service.websocket);

        const data = energyScheduler?.schedule?.result?.data ?? [];
        const predictionData = data.filter((e) => e.type === "PREDICTION");

        if (predictionData.length !== 0) {
            messages.push(
                this.getAutarchyMessage(), //
                this.getEvseSmartEventMessage(),
                this.getMinMaxGridPriceMessage(edge, config, predictionData),
            );
        }

        return messages.filter((e) => e !== null);
    }

    private getAutarchyMessage(): FenaMessage | null {
        return {
            text: "Heute bist du autark - nur mit Sonne und Stromspeicher!",
        };
    }

    private getEvseSmartEventMessage(): FenaMessage | null {
        const locale = Language.getCurrentLanguage().dateFnsLocale;
        const baseDate = new Date();
        const date = new Date();
        date.setHours(7, 0, 0, 0);
        if (date <= baseDate) {
            date.setDate(date.getDate() + 1);
        }
        const formattedTime = formatRelative(date, baseDate, { locale });

        return {
            text: "Dein E-Auto wird jetzt so günstig wie möglich beladen, damit Du " + formattedTime + " Uhr losfahren kannst",
            actions: [
                {
                    title: "Smart Event anpassen",
                    callback: () => this.router.navigate(["evse", "ctrlEvseSingle0"], { relativeTo: this.route }),
                },
                {
                    title: "Heute nicht laden",
                    callback: () => this.router.navigate(["evse", "ctrlEvseSingle0"], { relativeTo: this.route }),
                },
            ],
        };
    }

    private getMinMaxGridPriceMessage(edge: Edge, config: EdgeConfig, predictionData: GetSchedule.Response["result"]["data"]): FenaMessage | null {
        let min: { timestamp: string; value: number } | null = null;
        let max: { timestamp: string; value: number } | null = null;
        for (const d of predictionData) {
            const price = d._sum?.GridBuyPrice;
            if (price !== null && price !== undefined) {
                if (min == null || price < min.value) {
                    min = { timestamp: d.timestamp, value: price };
                }
                if (max == null || price > max.value) {
                    max = { timestamp: d.timestamp, value: price };
                }
            }
        }

        if (min == null || max == null) {
            return null;
        }

        const locale = Language.getCurrentLanguage().dateFnsLocale;
        const meta = config.getComponentSafely("_meta");
        const currency = config.getPropertyFromComponent<string>(meta, "currency");
        const currencyLabel: Currency.Label = Currency.getCurrencyLabelByCurrency(currency);
        const converter = Converter.CURRENCY_PER_KWH(currencyLabel);

        const baseDate = new Date();
        const formattedMaxTime = formatRelative(max.timestamp, baseDate, { locale });
        const formattedMinTime = formatRelative(min.timestamp, baseDate, { locale });
        const messageText =
            this.translate.instant("FENA.ELECTRICITY_PRICES_SUMMARY", {
                maxPrice: converter(max.value),
                maxTime: formattedMaxTime,
                minPrice: converter(min.value),
                minTime: formattedMinTime,
            }) ?? `Deine Strompreise:<br/>Der höchste ist <strong>${converter(max.value)}</strong> ${formattedMaxTime} Uhr.<br/>Der niedrigste ist <strong>${converter(min.value)}</strong> ${formattedMinTime} Uhr.`;

        return {
            text: messageText,
            actions: [
                {
                    title: this.translate.instant("GENERAL.DETAILS"),
                    callback: () => this.router.navigate(["common", "grid"], { relativeTo: this.route }),
                },
            ],
        };
    }
}
