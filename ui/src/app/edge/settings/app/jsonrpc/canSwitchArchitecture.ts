import { JsonrpcRequest, JsonrpcResponseSuccess } from "src/app/shared/jsonrpc/base";

export namespace CanSwitchArchitecture {

    export class Request extends JsonrpcRequest {

        public constructor(method: string) {
            super(method, {});
        }
    }

    export class Response extends JsonrpcResponseSuccess {
        public constructor(
            public override readonly id: string,
            public override readonly result: {
                canSwitch: boolean,
                header: string,
                info: string,
                link: string,
                current?: string,
            },
        ) {
            super(id, result);
        }
    }

}
