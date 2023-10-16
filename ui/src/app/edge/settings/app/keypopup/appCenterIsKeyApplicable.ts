import { JsonrpcRequest, JsonrpcResponseSuccess } from "src/app/shared/jsonrpc/base";

import { App } from "./app";

/**
 * Gets if a key can be redeemed.
 * 
 * <p>
 * Note: This Request needs to be wrapped in a appCenter Request.
 * 
 * <p>
 * Request:
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "isKeyApplicable",
 *   "params": {
 *     "key": string,
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
 *   "result": {
 *      isKeyApplicable: boolean,
 *      additionalInfo: {
 *          keyId: string,
 *          bundles: (App[])[],
 *          registrations: {
 *              edgeId: string,
 *              appId?: string
 *          }[],
 *          usages: {
 *              appId: string,
 *              installedInstances: number,
 *          }[]
 *      }
 *   }
 * }
 * </pre>
 */
export namespace AppCenterIsKeyApplicable {

    export const METHOD: string = "isKeyApplicable";

    export class Request extends JsonrpcRequest {

        public constructor(
            public override readonly params: {
                key: string,
                appId: string,
            }
        ) {
            super(METHOD, params);
        }
    }

    export class Response extends JsonrpcResponseSuccess {

        public constructor(
            public override readonly id: string,
            public override readonly result: {
                isKeyApplicable: boolean,
                additionalInfo: {
                    keyId: string,
                    bundles: (App[])[],
                    registrations: {
                        edgeId: string,
                        appId?: string
                    }[],
                    usages: {
                        appId: string,
                        installedInstances: number
                    }[]
                }
            }
        ) {
            super(id, result);
        }
    }

}
