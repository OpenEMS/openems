import { JsonrpcRequest } from "src/app/shared/jsonrpc/base";

/**
 * Wrapper for Requests specific to AppCenter.
 * 
 * <p>
 * Request:
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "appCenter",
 *   "params": {
 *     "payload": {} // the specific request to the AppCenter
 *   }
 * }
 * </pre>
 * 
 * <p>
 * Response:
 * 
 * <pre>
 * The response base on the request in the payload.
 * </pre>
 */
export namespace AppCenter {

    export const METHOD: string = "appCenter";

    export class Request extends JsonrpcRequest {

        public constructor(
            public override readonly params: {
                payload: JsonrpcRequest
            }
        ) {
            super(AppCenter.METHOD, params);
        }
    }

}
