import { JsonrpcNotification } from "../base";
import { SystemLog } from "../../type/systemlog";

/**
 * Represents a JSON-RPC Notification for sending the current system log.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "method": "systemLog",
 *   "params": {
 *     "line": {
 *       "time": string, // in the form '2011-12-03T10:15:30+01:00'
 *       "level": 'ERROR' | 'WARN' | 'INFO',
 *       "source": string,
 *       "message": string
 *     }
 *   }
 * }
 * </pre>
 */
export class SystemLogNotification extends JsonrpcNotification {

    public static readonly METHOD: string = "systemLog";

    public constructor(
        public override readonly params: {
            line: SystemLog
        },
    ) {
        super(SystemLogNotification.METHOD, params);
    }

}
