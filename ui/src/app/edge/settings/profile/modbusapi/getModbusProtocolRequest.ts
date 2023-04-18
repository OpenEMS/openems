import { JsonrpcRequest } from '../../../../shared/jsonrpc/base';

/**
 * Wraps a JSON-RPC Request to query the Modbus Protocol from Modbus/TCP
 * Api-Controller
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getModbusProtocol",
 *   "params": {}
 * }
 * </pre>
 */
export class GetModbusProtocolRequest extends JsonrpcRequest {

    private static METHOD: string = "getModbusProtocol";

    public constructor(
    ) {
        super(GetModbusProtocolRequest.METHOD, {});
    }

}