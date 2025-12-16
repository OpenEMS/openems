import { JsonrpcRequest, JsonrpcResponseSuccess } from "src/app/shared/jsonrpc/base";

export namespace SwitchArchitecture {

    export class Request extends JsonrpcRequest {

        public constructor(method: string) {
            super(method, {});
        }
    }

    export class Response extends JsonrpcResponseSuccess {
        public constructor(
            public override readonly id: string,
            public override readonly result: {
                instances: AppInstance[]
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
        dependencies: Dependency[]
    }

    export interface Dependency {
        key: string,
        instanceId: string
    }
}
