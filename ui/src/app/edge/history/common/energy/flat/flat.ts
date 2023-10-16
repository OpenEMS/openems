import { Component } from '@angular/core';
import { AbstractFlatWidget } from 'src/app/shared/genericComponents/flat/abstract-flat-widget';
import { ChannelAddress, CurrentData, Utils } from '../../../../../shared/shared';
import { Base64PayloadResponse } from 'src/app/shared/jsonrpc/response/base64PayloadResponse';
import { QueryHistoricTimeseriesExportXlxsRequest } from 'src/app/shared/jsonrpc/request/queryHistoricTimeseriesExportXlxs';
import { isSameDay, format, isSameMonth, isSameYear } from 'date-fns';
import { saveAs } from 'file-saver-es';

@Component({
    selector: 'energy',
    templateUrl: './flat.html'
})
export class FlatComponent extends AbstractFlatWidget {

    protected autarchyValue: number | null;
    private static readonly EXCEL_TYPE = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8';
    private static readonly EXCEL_EXTENSION = '.xlsx';
    protected readonly isSmartphoneResolution = this.service.isSmartphoneResolution;

    protected override onCurrentData(currentData: CurrentData) {
        this.autarchyValue =
            Utils.calculateAutarchy(
                currentData.allComponents['_sum/GridBuyActiveEnergy'] / 1000,
                currentData.allComponents['_sum/ConsumptionActiveEnergy'] / 1000);
    }

    protected override getChannelAddresses(): ChannelAddress[] {
        return [
            new ChannelAddress('_sum', 'GridBuyActiveEnergy'),
            new ChannelAddress('_sum', 'ConsumptionActiveEnergy')
        ];
    }

    public getChartHeight(): number {
        return this.service.deviceHeight / 2;
    }

    /**
 * Export historic data to Excel file.
 */
    protected exportToXlxs() {
        this.service.getCurrentEdge().then(edge => {
            edge.sendRequest(this.websocket, new QueryHistoricTimeseriesExportXlxsRequest(this.service.historyPeriod.value.from, this.service.historyPeriod.value.to)).then(response => {
                let r = response as Base64PayloadResponse;
                var binary = atob(r.result.payload.replace(/\s/g, ''));
                var len = binary.length;
                var buffer = new ArrayBuffer(len);
                var view = new Uint8Array(buffer);
                for (var i = 0; i < len; i++) {
                    view[i] = binary.charCodeAt(i);
                }
                const data: Blob = new Blob([view], {
                    type: FlatComponent.EXCEL_TYPE
                });

                let fileName = "Export-" + edge.id + "-";
                let dateFrom = this.service.historyPeriod.value.from;
                let dateTo = this.service.historyPeriod.value.to;
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

