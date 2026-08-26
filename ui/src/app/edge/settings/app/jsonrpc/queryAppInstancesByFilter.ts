import { JsonrpcRequest, JsonrpcResponseSuccess } from "../../../../shared/jsonrpc/base";

/**
 * Represents a JSON-RPC Request for 'queryAppInstancesByFilter'.
 *
 * <p>
 * Request:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "queryAppInstancesByFilter",
*    "filter": {
*       categorys?: string[],
*       components: {
*           componentId?: string,
*           factoryId?: string,
*        }
*       }
*       pagination: {
*        limit: number,
*       }
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
export namespace QueryAppInstancesByFilter {

    export const METHOD: string = "queryAppInstancesByFilter";

    export class Request extends JsonrpcRequest {

        public constructor(
            public override readonly params: {
                filter: {
                    categorys?: string[],
                    component: {
                        componentId?: string[],
                        factoryId?: string[],
                    }
                }
                pagination: {
                    limit: number,
                }
            },
        ) {
            super(METHOD, params);
        }
    }

    export class Response extends JsonrpcResponseSuccess {

        public constructor(
            public override readonly id: string,
            public override readonly result: {
                apps: AppInstance[]
            },
        ) {
            super(id, result);
        }
    }

    export interface AppInstance {
        appId: string,
        alias: string,
        instanceId: string,
        properties: Record<string, unknown>,
    }
}
