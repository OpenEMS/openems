import { JsonrpcRequest, JsonrpcResponseSuccess } from "../../../../shared/jsonrpc/base";

/**
 * Represents a JSON-RPC Request for 'getApps'.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getApps",
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
 *     "apps": {
 *       "appId": string,
 *       "name": string,
 *       "image: string (base64),
 *       "instanceIds": UUID[],
 *     }
 *   }
 * }
 * </pre>
 */
export namespace GetApps {

    export const METHOD: string = "getApps";

    export class Request extends JsonrpcRequest {

        public constructor(
        ) {
            super(METHOD, {});
        }
    }

    export class Response extends JsonrpcResponseSuccess {

        public constructor(
            public readonly id: string,
            public readonly result: {
                apps: App[]
            }
        ) {
            super(id, result);
        }
    }

    export interface App {
        category: 'INTEGRATED_SYSTEM',
        appId: string,
        name: string,
        image: string,
        instanceIds: string[],
    }
}