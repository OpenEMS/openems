import { Component, Input } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import * as FileSaver from 'file-saver';
import { ComponentJsonApiRequest } from '../../../shared/jsonrpc/request/componentJsonApiRequest';
import { EdgeConfig, Service, Websocket } from '../../../shared/shared';
import { GetModbusProtocolExportXlsxRequest } from './getModbusProtocolExportXlsxRequest';
import { Base64PayloadResponse } from '../../../shared/jsonrpc/response/base64PayloadResponse';

@Component({
  selector: ModbusApiComponent.SELECTOR,
  templateUrl: './modbusapi.component.html'
})
export class ModbusApiComponent {

  private static readonly SELECTOR = "modbusapi";
  private static readonly EXCEL_TYPE = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8';
  private static readonly EXCEL_EXTENSION = '.xlsx';

  @Input() private componentId: string;

  public controller: EdgeConfig.Component = null;

  constructor(
    private service: Service,
    private websocket: Websocket,
    private route: ActivatedRoute
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent('', this.route);
  }

  getModbusProtocol() {
    this.service.getCurrentEdge().then(edge => {
      let request = new ComponentJsonApiRequest({ componentId: this.componentId, payload: new GetModbusProtocolExportXlsxRequest() });
      edge.sendRequest(this.websocket, request).then(response => {
        let r = response as Base64PayloadResponse;

        // decode base64 string, remove space for IE compatibility
        // source: https://stackoverflow.com/questions/36036280/base64-representing-pdf-to-blob-javascript/45872086
        var binary = atob(r.result.payload.replace(/\s/g, ''));
        var len = binary.length;
        var buffer = new ArrayBuffer(len);
        var view = new Uint8Array(buffer);
        for (var i = 0; i < len; i++) {
          view[i] = binary.charCodeAt(i);
        }
        const data: Blob = new Blob([view], {
          type: ModbusApiComponent.EXCEL_TYPE
        });

        const fileName = "Modbus-TCP-" + edge.id;
        FileSaver.saveAs(data, fileName + ModbusApiComponent.EXCEL_EXTENSION);

      }).catch(reason => {
        console.warn(reason);
      })
    })
  }
}
