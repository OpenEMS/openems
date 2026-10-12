import { JsonrpcRequest, JsonrpcResponseSuccess } from "../../../../shared/jsonrpc/base";

/**
 * Gets all Updateables.
 *
 * Request:
 *
 * ```json
 * {
 *     "id": "UUID",
 *     "jsonrpc": "2.0",
 *     "method": "getUpdateables",
 *     "params": {}
 * }
 * ```
 *
 * Response:
 *
 * ```json
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "updateables": Updateable[]
 *   }
 * }
 * ```
 */
export namespace GetUpdateables {
    export const METHOD: string = "getUpdateables";

    export class Request extends JsonrpcRequest {
        public constructor() {
            super(METHOD, {});
        }
    }

    export class Response extends JsonrpcResponseSuccess {
        public constructor(
            public override readonly id: string,
            public override readonly result: {
                updateables: Updateable[];
            },
        ) {
            super(id, result);
        }
    }
}

export type Updateable = {
    id: string;
    name: string;
    description: string;
    requiredMinRole?: string | null;
    avgDurationInMinutes?: number;
};
