import { JsonrpcResponseSuccess } from '../../../../shared/jsonrpc/base';

/**
 * Wraps a JSON-RPC Response to "getModbusProtocol" Request
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "table": [{
 *       "ref": number // start address of the Modbus Record
 *       "name": string,
 *       "value": string, // value description
 *       "unit": string,
 *       "type" string
 *     }]
 *   }
 * }
 * </pre>
 */
export class GetModbusProtocolResponse extends JsonrpcResponseSuccess {

    public constructor(
        public readonly id: string,
        public readonly result: {
            table: [{
                ref: number,
                name: string,
                value: string,
                unit: string,
                type: string
            }]
        },
    ) {
        super(id, result);
    }
}
