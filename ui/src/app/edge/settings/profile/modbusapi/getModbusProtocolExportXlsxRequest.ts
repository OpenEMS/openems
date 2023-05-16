import { JsonrpcRequest } from '../../../../shared/jsonrpc/base';

/**
 * Wraps a JSON-RPC Request to query the Modbus Protocol from Modbus/TCP
 * Api-Controller
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getModbusProtocolXlsx",
 *   "params": {}
 * }
 * </pre>
 */
export class GetModbusProtocolExportXlsxRequest extends JsonrpcRequest {

    public static METHOD: string = "getModbusProtocolExportXlsx";

    public constructor(
    ) {
        super(GetModbusProtocolExportXlsxRequest.METHOD, {});
    }

}