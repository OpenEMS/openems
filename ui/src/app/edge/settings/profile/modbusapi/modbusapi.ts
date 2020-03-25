import * as FileSaver from 'file-saver';
import { ComponentJsonApiRequest } from 'src/app/shared/jsonrpc/request/componentJsonApiRequest';
import { Base64PayloadResponse } from 'src/app/shared/jsonrpc/response/base64PayloadResponse';
import { Service } from 'src/app/shared/shared';
import { GetModbusProtocolExportXlsxRequest } from './getModbusProtocolExportXlsxRequest';

export class ModbusApiUtil {

    private static readonly EXCEL_TYPE = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8';
    private static readonly EXCEL_EXTENSION = '.xlsx';

    public static getModbusProtocol(service: Service, componentId: string) {
        service.getCurrentEdge().then(edge => {
            let request = new ComponentJsonApiRequest({ componentId: componentId, payload: new GetModbusProtocolExportXlsxRequest() });
            edge.sendRequest(service.websocket, request).then(response => {
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
                    type: ModbusApiUtil.EXCEL_TYPE
                });

                const fileName = "Modbus-TCP-" + edge.id;
                FileSaver.saveAs(data, fileName + ModbusApiUtil.EXCEL_EXTENSION);

            }).catch(reason => {
                console.warn(reason);
            })
        })
    }
} 