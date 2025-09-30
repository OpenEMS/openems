import { JsonrpcRequest } from "../base";

/**
 * <pre>
 * {
 *  "jsonrpc": "2.0",
 *  "id": UUID,
 *  "method": "getUserInformation"
 *  }
 * </pre>
 */
export class GetUserInformationRequest extends JsonrpcRequest {

    private static METHOD: string = "getUserInformation";

    public constructor() {
        super(GET_USER_INFORMATION_REQUEST.METHOD, {});
    }

}
