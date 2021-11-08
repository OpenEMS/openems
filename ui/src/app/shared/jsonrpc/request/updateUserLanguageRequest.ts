import { JsonrpcRequest } from "../base";

/**
 * 
 * Represents a JSON-RPC Response for a {@link UpdateUserLanguageRequest}.
 * <pre>
 * {
 *   "method": "updateUserLanguage",
 *   "id": UUID,
 *   "params": {
 *      "language": string
 *   }
 * }
 * </pre>
 */
export class UpdateUserLanguageRequest extends JsonrpcRequest {

    static METHOD: string = "updateUserLanguage";

    public constructor(
        public readonly params: {
            language: string
        }
    ) {
        super(UpdateUserLanguageRequest.METHOD, params);
    }

}