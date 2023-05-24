import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { isSameYear } from 'date-fns';
import { format, isSameDay, isSameMonth } from 'date-fns/esm';
import { saveAs } from 'file-saver-es';
import { QueryHistoricTimeseriesExportXlxsRequest } from 'src/app/shared/jsonrpc/request/queryHistoricTimeseriesExportXlxs';
import { Base64PayloadResponse } from 'src/app/shared/jsonrpc/response/base64PayloadResponse';
import { Service, Websocket } from '../../../../shared/shared';

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
                    type: EnergyModalComponent.EXCEL_TYPE
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
                fileName += EnergyModalComponent.EXCEL_EXTENSION;
                saveAs(data, fileName);

            }).catch(reason => {
                console.warn(reason);
            });
        });
    }
}
