import { JsonrpcResponseSuccess } from "src/app/shared/jsonrpc/base";

export interface SystemUpdateState {
    unknown?: {},
    updated?: { version: string },
    available?: {
        currentVersion: string,
        latestVersion: string
    },
    running?: {
        percentCompleted: number,
        logs: string[]
    }
}

/**
 * JSON-RPC Response to "getSystemUpdateState" Request.
 * 
 * <p>
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     // State is unknown (e.g. internet connection limited by firewall)
 *      // TODO remove unknown? Throw exception instead
 *     "unknown"?: {
 *     }
 *     // Latest version is already installed
 *     "updated"?: {
 *       "version": "XXXX"
 *     }
 *     // Update is available
 *     "available"?: {
 *       "currentVersion": "XXXX",
 *       "latestVersion": "XXXX"
 *     },
 *     // Update is currently running
 *     "running"?: {
 *       "percentCompleted": number,
 *       "logs": string[]
 *     }
 *   }
 * }
 * </pre>
 */
export class GetSystemUpdateStateResponse extends JsonrpcResponseSuccess {

    public constructor(
        public override readonly id: string,
        public override readonly result: SystemUpdateState
    ) {
        super(id, result);
    }
}
