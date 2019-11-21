import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { isSameYear } from 'date-fns';
import { format, isSameDay, isSameMonth } from 'date-fns/esm';
import * as FileSaver from 'file-saver';
import { QueryHistoricTimeseriesExportXlxsRequest } from 'src/app/shared/jsonrpc/request/queryHistoricTimeseriesExportXlxs';
import { Base64PayloadResponse } from 'src/app/shared/jsonrpc/response/base64PayloadResponse';
import { ChannelAddress, Service, Websocket } from '../../../../shared/shared';
import { ModalController } from '@ionic/angular';

@Component({
    selector: EnergyModalComponent.SELECTOR,
    templateUrl: './modal.component.html'
})
export class EnergyModalComponent implements OnInit {

    private static readonly SELECTOR = "energy-modal";

    private static readonly EXCEL_TYPE = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8';
    private static readonly EXCEL_EXTENSION = '.xlsx';

    constructor(
        protected service: Service,
        private websocket: Websocket,
        private route: ActivatedRoute,
        public translate: TranslateService,
        public modalCtrl: ModalController,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route);
    }

    /**
     * Export historic data to Excel file.
     */
    public exportToXlxs() {
        this.service.getCurrentEdge().then(edge => {
            // TODO the order of these channels should be reflected in the excel file
            let dataChannels = [
                // Storage Soc
                new ChannelAddress('_sum', 'EssActivePower'),
                // Storage
                new ChannelAddress('_sum', 'EssActivePower'),
                // Grid
                new ChannelAddress('_sum', 'GridActivePower'),
                // Production
                new ChannelAddress('_sum', 'ProductionActivePower'),
                // Consumption
                new ChannelAddress('_sum', 'ConsumptionActivePower')
            ];
            let energyChannels = [
                // new ChannelAddress('_sum', 'EssSoc'),
                // new ChannelAddress('_sum', 'GridBuyActiveEnergy'),
                // new ChannelAddress('_sum', 'GridSellActiveEnergy'),
                // new ChannelAddress('_sum', 'ProductionActiveEnergy'),
                // new ChannelAddress('_sum', 'ConsumptionActiveEnergy'),
                // new ChannelAddress('_sum', 'EssActiveChargeEnergy'),
                // new ChannelAddress('_sum', 'EssActiveDischargeEnergy')
            ];
            edge.sendRequest(this.websocket, new QueryHistoricTimeseriesExportXlxsRequest(this.service.historyPeriod.from, this.service.historyPeriod.to, dataChannels, energyChannels)).then(response => {
                let r = response as Base64PayloadResponse;
                var binary = atob(r.result.payload.replace(/\s/g, ''));
                var len = binary.length;
                var buffer = new ArrayBuffer(len);
                var view = new Uint8Array(buffer);
                for (var i = 0; i < len; i++) {
                    view[i] = binary.charCodeAt(i);
                }
                const data: Blob = new Blob([view], {
                    type: EnergyModalComponent.EXCEL_TYPE
                });

                let fileName = "Export-" + edge.id + "-";
                let dateFrom = this.service.historyPeriod.from;
                let dateTo = this.service.historyPeriod.to;
                if (isSameDay(dateFrom, dateTo)) {
                    fileName += format(dateFrom, "dd.MM.yyyy");
                } else if (isSameMonth(dateFrom, dateTo)) {
                    fileName += format(dateFrom, "dd.") + "-" + format(dateTo, "dd.MM.yyyy");
                } else if (isSameYear(dateFrom, dateTo)) {
                    fileName += format(dateFrom, "dd.MM.") + "-" + format(dateTo, "dd.MM.yyyy");
                } else {
                    fileName += format(dateFrom, "dd.MM.yyyy") + "-" + format(dateTo, "dd.MM.yyyy");
                }
                fileName += EnergyModalComponent.EXCEL_EXTENSION;
                FileSaver.saveAs(data, fileName);

            }).catch(reason => {
                console.warn(reason);
            })
        })
    }
}
