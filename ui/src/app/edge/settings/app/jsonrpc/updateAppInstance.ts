import { JsonrpcRequest, JsonrpcResponseSuccess } from "../../../../shared/jsonrpc/base";
import { GetAppInstances } from "./getAppInstances";

/**
 * Updates an instance of an {@link OpenemsApp}.
 *
 * <p>
 * Request:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "updateAppInstance",
 *   "params": {
 *     "instanceId": string (uuid),
 *     "alias": "alias",
 *     "properties": {}
 *   }
 * }
 * </pre>
 *
 * <p>
 * Response:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {}
 * }
 * </pre>
 */
export namespace UpdateAppInstance {

    export const METHOD: string = "updateAppInstance";

    export class Request extends JsonrpcRequest {

        public constructor(
            public override readonly params: {
                instanceId: string,
                alias: string,
                properties: {}
            },
        ) {
            super(METHOD, params);
        }
    }


    export class Response extends JsonrpcResponseSuccess {

        public constructor(
            public override readonly id: string,
            public override readonly result: {
                instance: GetAppInstances.AppInstance,
                warnings: string[]
            },
        ) {
            super(id, result);
        }
    }

}
