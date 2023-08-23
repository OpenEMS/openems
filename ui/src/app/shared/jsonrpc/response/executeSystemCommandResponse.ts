import { JsonrpcResponseSuccess } from "../base";

export interface Cumulated {
    [channelAddress: string]: number | null
}

/**
 * JSON-RPC Response to "executeSystemCommand" Request.
 * 
 * <p>
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "stdout": string[],
 *     "stderr": string[]
 *   }
 * }
 * </pre>
 */
export class ExecuteSystemCommandResponse extends JsonrpcResponseSuccess {

    public constructor(
        public override readonly id: string,
        public override readonly result: {
            stdout: string[],
            stderr: string[]
        }
    ) {
        super(id, result);
    }
}