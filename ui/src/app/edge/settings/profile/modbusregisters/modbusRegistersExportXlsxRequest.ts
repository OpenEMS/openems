import { JsonrpcRequest } from '../../../../shared/jsonrpc/base';

/**
 * Exports the Modbus Registers and current Channel-Value to an Excel (xlsx)
 * file.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "modbusRegistersExportXlsx",
 *   "params": {}
 * }
 * </pre>
 */
export class ModbusRegistersExportXlsxRequest extends JsonrpcRequest {

    static METHOD: string = "modbusRegistersExportXlsx";

    public constructor(
    ) {
        super(ModbusRegistersExportXlsxRequest.METHOD, {});
    }

}