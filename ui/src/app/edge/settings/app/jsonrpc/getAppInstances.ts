import { JsonrpcRequest, JsonrpcResponseSuccess } from "../../../../shared/jsonrpc/base";

/**
 * Represents a JSON-RPC Request for 'getAppInstances'.
 * 
 * <p>
 * Request:
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getAppInstances",
 *   "params": {
 *     "appId": string
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
 *   "alias": "alias",
 *   "result": {
 *     "instances": AppInstance[]
 *   }
 * }
 * </pre>
 */
export namespace GetAppInstances {

    export const METHOD: string = "getAppInstances";

    export class Request extends JsonrpcRequest {

        public constructor(
            public readonly params: {
                appId: string
            }
        ) {
            super(METHOD, params);
        }
    }

    export class Response extends JsonrpcResponseSuccess {

        public constructor(
            public readonly id: string,
            public readonly result: {
                instances: AppInstance[]
            }
        ) {
            super(id, result);
        }
    }

    export interface AppInstance {
        appId: string,
        alias: string,
        instanceId: string,
        properties: {},
        dependencies: Dependency[]
    }

    export interface Dependency {
        key: string,
        instanceId: string
    }
}