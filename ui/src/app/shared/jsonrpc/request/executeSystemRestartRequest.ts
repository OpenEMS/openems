import { JsonrpcRequest } from "../base";

/**
 * Represents a JSON-RPC Request to execute a system restart.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "executeSystemRestart",
 *   "params": {
 *      "type": "SOFT" | "HARD",
 *   }
 * }
 * </pre>
 */
export class ExecuteSystemRestartRequest extends JsonrpcRequest {

    private static METHOD: string = "executeSystemRestart";

    public constructor(
        public override readonly params: {
            type: Type,
        },
    ) {
        super(ExecuteSystemRestartRequest.METHOD, params);
    }

}

export enum Type {
    SOFT = "SOFT",
    HARD = "HARD",
}
