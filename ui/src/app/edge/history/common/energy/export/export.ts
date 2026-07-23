import { Component, computed, effect, inject, signal } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { format, isSameDay, isSameMonth, isSameYear } from "date-fns";
import { saveAs } from "file-saver-es";
import { v4 as uuidv4 } from "uuid";
import { NavigationCard } from "src/app/edge/live/navigation-info/navigation-info";
import { PlatFormService } from "src/app/platform.service";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";

import { PickdateComponentModule } from "src/app/shared/components/pickdate/pickdate.module";
import { QueryHistoricTimeseriesExportXlxsRequest } from "src/app/shared/jsonrpc/request/queryHistoricTimeseriesExportXlxs";
import { Base64PayloadResponse } from "src/app/shared/jsonrpc/response/base64PayloadResponse";
import { Service } from "src/app/shared/shared";
import { Language } from "src/app/shared/type/language";
import { DateUtils } from "src/app/shared/utils/date/dateutils";
import de from "./i18n/de.json";
import en from "./i18n/en.json";

export type NavigationCardWithAction = Omit<NavigationCard, "buttonHref" | "infoText" | "footer"> & {
    buttonCallback: () => void;
    buttonIconName: string;
};

@Component({
    selector: "oe-history-export",
    templateUrl: "./export.html",
    standalone: true,
    imports: [CommonUiModule, ComponentsBaseModule, PickdateComponentModule],
})
export class HistoryExcelExportComponent {
    private static readonly EXCEL_TYPE =
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8";
    private static readonly EXCEL_EXTENSION = ".xlsx";

    protected readonly translate = inject(TranslateService);
    protected readonly cards = signal<NavigationCardWithAction[]>([]);

    protected readonly spinnerId: string = uuidv4();
    protected readonly service = inject(Service);
    protected isExportRunning = signal<boolean>(false);
    protected edge = computed(() => this.service.currentEdge());
    protected isInitialized = signal<boolean>(false);
    private readonly platFormService = inject(PlatFormService);

    constructor() {
        Language.normalizeAdditionalTranslationFiles({ de: de, en: en }).then((translations) => {
            for (const { lang, translation, shouldMerge } of translations) {
                this.translate.setTranslation(lang, translation, shouldMerge);
            }
            this.isInitialized.set(true);
            this.setCards();
        });

        effect(() => {
            const isExportRunning = this.isExportRunning();
            if (isExportRunning) {
                this.service.startSpinner(this.spinnerId);
            } else {
                this.service.stopSpinner(this.spinnerId);
            }
        });
    }

    setCards() {
        this.cards.set([
            {
                iconName: "download-outline",
                contentText: this.translate.instant("HISTORY_EXPORT.DESCRIPTION"),
                buttonText: this.translate.instant("HISTORY_EXPORT.DOWNLOAD_AS_XLSX"),
                buttonCallback: () => this.exportToXlxs(),
                buttonIconName: "download-outline",
            },
        ]);
    }

    /** Export historic data to Excel file. */
    protected exportToXlxs() {
        if (this.platFormService.getDevice()?.isApp()) {
            this.service.toast(this.translate.instant("APP.FUNCTIONALITY_TEMPORARILY_NOT_AVAILABLE"), "warning");
            return;
        }

        const edge = this.service.currentEdge();

        if (edge == null) {
            return;
        }

        this.isExportRunning.set(true);
        const maxDate = DateUtils.maxDate(this.service.historyPeriod.value.from, edge?.firstSetupProtocol ?? null);

        if (maxDate == null) {
            this.isExportRunning.set(false);
            return;
        }

        edge.sendRequest(
            this.service.websocket,
            new QueryHistoricTimeseriesExportXlxsRequest(maxDate, this.service.historyPeriod.value.to),
        )
            .then((response) => {
                const r = response as Base64PayloadResponse;
                const binary = atob(r.result.payload.replace(/\s/g, ""));
                const len = binary.length;
                const buffer = new ArrayBuffer(len);
                const view = new Uint8Array(buffer);
                for (let i = 0; i < len; i++) {
                    view[i] = binary.charCodeAt(i);
                }
                const data: Blob = new Blob([view], {
                    type: HistoryExcelExportComponent.EXCEL_TYPE,
                });

                let fileName = "Export-" + edge.id + "-";
                const dateFrom = this.service.historyPeriod.value.from;
                const dateTo = this.service.historyPeriod.value.to;
                if (isSameDay(dateFrom, dateTo)) {
                    fileName += format(dateFrom, "dd.MM.yyyy");
                } else if (isSameMonth(dateFrom, dateTo)) {
                    fileName += format(dateFrom, "dd.") + "-" + format(dateTo, "dd.MM.yyyy");
                } else if (isSameYear(dateFrom, dateTo)) {
                    fileName += format(dateFrom, "dd.MM.") + "-" + format(dateTo, "dd.MM.yyyy");
                } else {
                    fileName += format(dateFrom, "dd.MM.yyyy") + "-" + format(dateTo, "dd.MM.yyyy");
                }
                fileName += HistoryExcelExportComponent.EXCEL_EXTENSION;
                saveAs(data, fileName);
            })
            .catch((reason) => {
                console.warn(reason);
            })
            .finally(() => {
                this.isExportRunning.set(false);
            });
    }
}
