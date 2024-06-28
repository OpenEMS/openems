// @ts-strict-ignore
import { Component } from '@angular/core';
import { AbstractFlatWidget } from 'src/app/shared/genericComponents/flat/abstract-flat-widget';
import { ChannelAddress, CurrentData, Utils } from '../../../../../shared/shared';
import { Base64PayloadResponse } from 'src/app/shared/jsonrpc/response/base64PayloadResponse';
import { QueryHistoricTimeseriesExportXlxsRequest } from 'src/app/shared/jsonrpc/request/queryHistoricTimeseriesExportXlxs';
import { isSameDay, format, isSameMonth, isSameYear } from 'date-fns';
import { saveAs } from 'file-saver-es';
import { AppService } from 'src/app/app.service';

@Component({
    selector: 'energy',
    templateUrl: './flat.html',
})
export class FlatComponent extends AbstractFlatWidget {

    protected autarchyValue: number | null;
    private static readonly EXCEL_TYPE = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8';
    private static readonly EXCEL_EXTENSION = '.xlsx';
    protected readonly isSmartphoneResolution = this.service.isSmartphoneResolution;
    protected readonly isApp: boolean = AppService.isApp;

    protected override onCurrentData(currentData: CurrentData) {
        this.autarchyValue =
            Utils.calculateAutarchy(
                currentData.allComponents['_sum/GridBuyActiveEnergy'] / 1000,
                currentData.allComponents['_sum/ConsumptionActiveEnergy'] / 1000);
    }

    protected override getChannelAddresses(): ChannelAddress[] {
        return [
            new ChannelAddress('_sum', 'GridBuyActiveEnergy'),
            new ChannelAddress('_sum', 'ConsumptionActiveEnergy'),
        ];
    }

    public getChartHeight(): number {
        return this.service.deviceHeight / 2;
    }

    /**
 * Export historic data to Excel file.
 */
    protected exportToXlxs() {

        if (AppService.isApp) {
            this.service.toast(this.translate.instant('APP.FUNCTIONALITY_TEMPORARILY_NOT_AVAILABLE'), "warning");
            return;
        }

        this.service.getCurrentEdge().then(edge => {
            edge.sendRequest(this.websocket, new QueryHistoricTimeseriesExportXlxsRequest(this.service.historyPeriod.value.from, this.service.historyPeriod.value.to)).then(response => {
                const r = response as Base64PayloadResponse;
                const binary = atob(r.result.payload.replace(/\s/g, ''));
                const len = binary.length;
                const buffer = new ArrayBuffer(len);
                const view = new Uint8Array(buffer);
                for (let i = 0; i < len; i++) {
                    view[i] = binary.charCodeAt(i);
                }
                const data: Blob = new Blob([view], {
                    type: FlatComponent.EXCEL_TYPE,
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
                fileName += FlatComponent.EXCEL_EXTENSION;
                saveAs(data, fileName);

            }).catch(reason => {
                console.warn(reason);
            });
        });
    }
}

