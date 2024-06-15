import { EdgeConfig } from '../../edge/edgeconfig';
import { JsonrpcNotification } from "../base";

/**
 * Represents a JSON-RPC Notification for new EdgeConfig.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "method": "edgeConfig",
 *   "params": EdgeConfig
 * }
 * </pre>
 */
export class EdgeConfigNotification extends JsonrpcNotification {

    public static readonly METHOD: string = "edgeConfig";

    public constructor(
        public override readonly params: EdgeConfig,
    ) {
        super(EdgeConfigNotification.METHOD, params);
    }

}
