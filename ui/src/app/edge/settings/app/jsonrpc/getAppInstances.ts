import { States } from "src/app/shared/states/states";
import { JsonrpcRequest, JsonrpcResponseSuccess } from "../../../../shared/jsonrpc/base";

/**
 * Represents a JSON-RPC Request for 'getAppInstances'.
 *
 * @typedef {"jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getAppInstances",
 *   "params": {
 *     "appId": string
 *   }} Request
 *
 *
 * @typedef {"jsonrpc": "2.0", "id": "UUID", "alias": "alias", "result": { "instances": AppInstance[] }} Response
 */
export namespace GetAppInstances {
    export const METHOD: string = "getAppInstances";

    export class Request extends JsonrpcRequest {
        protected override requiredState: States = States.EDGE_SELECTED;

        public constructor(
            public override readonly params: {
                appId: string;
            },
        ) {
            super(METHOD, params);
        }
    }

    export class Response extends JsonrpcResponseSuccess {
        public constructor(
            public override readonly id: string,
            public override readonly result: {
                instances: AppInstance[];
            },
        ) {
            super(id, result);
        }
    }

    export interface AppInstance {
        appId: string;
        alias: string;
        instanceId: string;
        properties: Record<string, unknown>;
        dependencies: Dependency[];
    }

    export interface Dependency {
        key: string;
        instanceId: string;
    }
}
