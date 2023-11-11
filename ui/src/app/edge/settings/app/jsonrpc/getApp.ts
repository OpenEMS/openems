import { JsonrpcRequest, JsonrpcResponseSuccess } from "../../../../shared/jsonrpc/base";
import { GetApps } from "./getApps";

/**
 * Represents a JSON-RPC Request for 'getApps'.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getApp",
 *   "params": {}
 * }
 * </pre>
 * 
 * <p>
 * Response:
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "app": {
 *       "appId": string,
 *       "category": {
 *          "name": string,
 *          "readableName": string
 *       },
 *       "name": string,
 *       "usage": string,
 *       "status": {
 *          "status": string,
 *          "errorCompatibleMessages": string[],
 *          "errorInstallableMessages": string[]
 *       },
 *       "image: string (base64),
 *       "instanceIds": UUID[]
 *     }
 *   }
 * }
 * </pre>
 */
export namespace GetApp {

    export const METHOD: string = "getApp";

    export class Request extends JsonrpcRequest {

        public constructor(
            public override readonly params: {
                appId: string
            }
        ) {
            super(METHOD, {});
        }
    }

    export class Response extends JsonrpcResponseSuccess {

        public constructor(
            public override readonly id: string,
            public override readonly result: {
                app: GetApps.App
            }
        ) {
            super(id, result);
        }
    }

}
