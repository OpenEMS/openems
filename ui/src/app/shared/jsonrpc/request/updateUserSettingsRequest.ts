import { JsonrpcRequest } from "../base";

/**
 * Represents a JSON-RPC Response for a {@link UpdateUserSettingsRequest}.
 *
 * <pre>
 * {
 *   "method": "updateUserSettings",
 *   "id": UUID,
 *   "params": {
 *      "settings": {}
 *   }
 * }
 * </pre>
 */
export class UpdateUserSettingsRequest extends JsonrpcRequest {

    private static METHOD: string = "updateUserSettings";

    public constructor(
        public override readonly params: {
            settings: {}
        },
    ) {
        super(UpdateUserSettingsRequest.METHOD, params);
    }

}
