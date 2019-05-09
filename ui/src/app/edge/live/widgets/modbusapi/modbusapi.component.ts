import { Component, Input } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import * as FileSaver from 'file-saver';
import * as XLSX from 'xlsx';
import { ComponentJsonApiRequest } from '../../../../shared/jsonrpc/request/componentJsonApiRequest';
import { EdgeConfig, Service, Websocket } from '../../../../shared/shared';
import { GetModbusProtocolRequest } from './getModbusProtocolRequest';
import { GetModbusProtocolResponse } from './getModbusProtocolResponse';

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
      let request = new ComponentJsonApiRequest({ componentId: this.componentId, payload: new GetModbusProtocolRequest() });
      edge.sendRequest(this.websocket, request).then(response => {
        let r = response as GetModbusProtocolResponse;
        const worksheet: XLSX.WorkSheet = XLSX.utils.json_to_sheet(r.result.table);
        const workbook: XLSX.WorkBook = { Sheets: { 'data': worksheet }, SheetNames: ['data'] };
        const excelBuffer: any = XLSX.write(workbook, { bookType: 'xlsx', type: 'array' });
        const data: Blob = new Blob([excelBuffer], {
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
