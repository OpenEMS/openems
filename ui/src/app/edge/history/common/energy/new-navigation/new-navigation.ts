import { Component, computed, effect, inject, signal } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { isSameDay, format, isSameMonth, isSameYear } from "date-fns";
import { saveAs } from "file-saver-es";
import { v4 as uuidv4 } from "uuid";
import { PlatFormService } from "src/app/platform.service";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { FlatWidgetButtonComponent } from "src/app/shared/components/flat/flat-widget-button/flat-widget-button";

import { QueryHistoricTimeseriesExportXlxsRequest } from "src/app/shared/jsonrpc/request/queryHistoricTimeseriesExportXlxs";
import { Base64PayloadResponse } from "src/app/shared/jsonrpc/response/base64PayloadResponse";
import { Service } from "src/app/shared/shared";
import { DateUtils } from "src/app/shared/utils/date/dateutils";
import { ChartComponent } from "../chart/chart";

@Component({
    selector: "oe-history",
    templateUrl: "./new-navigation.html",
    standalone: true,
    imports: [CommonUiModule, ComponentsBaseModule, ChartComponent, FlatWidgetButtonComponent],
})
export class HistoryChartComponent {
    private static readonly EXCEL_TYPE =
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8";
    private static readonly EXCEL_EXTENSION = ".xlsx";

    protected readonly spinnerId: string = uuidv4();
    protected isExportRunning = signal<boolean>(false);
    protected readonly service = inject(Service);
    protected edge = computed(() => this.service.currentEdge());
    private readonly platFormService = inject(PlatFormService);
    private readonly translate = inject(TranslateService);

    constructor() {
        effect(() => {
            const isExportRunning = this.isExportRunning();
            if (isExportRunning) {
                this.service.startSpinner(this.spinnerId);
            } else {
                this.service.stopSpinner(this.spinnerId);
            }
        });
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
                    type: HistoryChartComponent.EXCEL_TYPE,
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
                fileName += HistoryChartComponent.EXCEL_EXTENSION;
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
