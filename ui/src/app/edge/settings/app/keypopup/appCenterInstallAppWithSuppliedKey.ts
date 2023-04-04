import { JsonrpcRequest, JsonrpcResponseSuccess } from "src/app/shared/jsonrpc/base";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { AddAppInstance } from "../jsonrpc/addAppInstance";
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
 * Response: AddAppInstance#Response
 */
export namespace AppCenterInstallAppWithSuppliedKeyRequest {

    export const METHOD: string = "installAppWithSuppliedKey";

    export class Request extends JsonrpcRequest {

        public constructor(
            public readonly params: {
                installRequest: JsonrpcRequest
            }
        ) {
            super(METHOD, params);
        }
    }

}
