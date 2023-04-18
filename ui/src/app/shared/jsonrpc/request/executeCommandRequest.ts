import { JsonrpcRequest } from "../base";

/**
 * Represents a JSON-RPC Request to execute a system command on OpenEMS Edge.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "executeSystemCommand",
 *   "params": {
 *   	"command": string,
 *   	"runInBackground"?: boolean = false, // run the command in background (true) or in foreground (false)
 *   	"timeoutSeconds"?: number = 5, // interrupt the command after ... seconds
 *   	"username"?: string,
 *   	"password"?: string,
 *   }
 * }
 * </pre>
 */
export class ExecuteSystemCommandRequest extends JsonrpcRequest {

    private static METHOD: string = "executeSystemCommand";

    public constructor(
        public readonly params: {
            command: string,
            runInBackground: boolean,
            timeoutSeconds: number,
            username?: string,
            password?: string
        }
    ) {
        super(ExecuteSystemCommandRequest.METHOD, params);
    }

}