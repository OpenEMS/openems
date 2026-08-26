import { JsonrpcRequest, JsonrpcResponseSuccess } from "src/app/shared/jsonrpc/base";

/**
 * Gets all OAuth provider.
 *
 * <p>
 * Request:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getAllOAuthProvider",
 *   "params": {}
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
 *      "metaInfos": OAuthMetaInfo[]
 *   }
 * }
 * </pre>
 */
export namespace GetAllOAuthProvider {

    export const METHOD: string = "getAllOAuthProvider";

    export class Request extends JsonrpcRequest {

        public constructor(
            public override readonly params: {} = {},
        ) {
            super(METHOD, params);
        }
    }

    export class Response extends JsonrpcResponseSuccess {

        public constructor(
            public override readonly id: string,
            override readonly result: {
                metaInfos: OAuthMetaInfo[],
            }
        ) {
            super(id, result);
        }

    }

}

export type OAuthMetaInfo = {
    identifier: string,
    title: string,
    description: string,
};
