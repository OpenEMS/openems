import { JsonrpcRequest, JsonrpcResponseSuccess } from "../../../../shared/jsonrpc/base";
import { GetAppInstances } from "./getAppInstances";

/**
 * Adds an OpenemsAppInstance.
 *
 * <p>
 * Request:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "alias" : "alias",
 *   "method": "addAppInstance",
 *   "params": {
 *     "appId": string,
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
 *   "result": {
 *     "instanceId": string (uuid)
 *   }
 * }
 * </pre>
 */
export namespace AddAppInstance {

    export const METHOD: string = "addAppInstance";

    export class Request extends JsonrpcRequest {

        public constructor(
            public override readonly params: {
                key?: string, // only for newer versions
                appId: string,
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
                instanceId: string,
                instance: GetAppInstances.AppInstance,
                warnings: String[]
            },
        ) {
            super(id, result);
        }
    }
}
