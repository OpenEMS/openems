import { Level } from "../../service/logger";
import { JsonrpcRequest } from "../base";

/**
 * <pre>
 * {
 *  "jsonrpc": "2.0",
 *  "id": UUID,
 *  "method": "sendLogMessage"
 *  "params": {
 *      "level": Level,
 *      "msg": string
 *  }
 * }
 * </pre>
 */
export class SendLogMessage extends JsonrpcRequest {

    static METHOD: string = "sendLogMessage";

    public constructor(
        public readonly params: {
            level: Level,
            msg: string
        }
    ) {
        super(SendLogMessage.METHOD, params);
    }

}