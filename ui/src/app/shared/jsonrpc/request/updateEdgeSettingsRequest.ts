import { JsonrpcRequest } from "../base";

/**
 * Represents a JSON-RPC Request to update the settings of an EMS.
 *
 * ```json
 * {
 *   "method": "updateEmsSettings",
 *   "id": UUID,
 *   "params": { "settings": {} }
 * }
 * ```
 */
export class UpdateEdgeSettingsRequest extends JsonrpcRequest {
    private static METHOD: string = "updateEdgeSettings";

    public constructor(
        public override readonly params: {
            edgeId: string;
            settings: {};
        },
    ) {
        super(UpdateEdgeSettingsRequest.METHOD, params);
    }
}
