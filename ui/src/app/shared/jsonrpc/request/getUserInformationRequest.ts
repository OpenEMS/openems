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

    static METHOD: string = "getUserInformation";

    public constructor() {
        super(GetUserInformationRequest.METHOD, {});
    }

}