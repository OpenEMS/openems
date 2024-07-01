import { Level } from "../../service/logger";
import { JsonrpcNotification } from "../base";

/**
 * <pre>
 * {
 *  "jsonrpc": "2.0",
 *  "id": UUID,
 *  "method": "logMessage"
 *  "params": {
 *      "level": Level,
 *      "msg": string
 *  }
 * }
 * </pre>
 */
export class LogMessageNotification extends JsonrpcNotification {

    public static METHOD: string = "logMessage";

    public constructor(
        public override readonly params: {
            level: Level,
            msg: string
        },
    ) {
        super(LogMessageNotification.METHOD, params);
    }

}
