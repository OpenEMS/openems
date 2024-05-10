import { JsonrpcResponseSuccess } from "../base";

/**
 * JSON-RPC Response to a "executeSystemRestart" Request.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "stdout": string[],
 *     "stderr": string[],
 *     "exitcode": number (exit code of application: 0 = successful; otherwise error)
 *   }
 * }
 * </pre>
 */
export class ExecuteSystemRestartResponse extends JsonrpcResponseSuccess {

    public constructor(
        public override readonly id: string,
        public override readonly result: string,
    ) {
        super(id, result);
    }

}
