import { GeoResult } from "src/app/shared/components/system-location-validator/system-location-validator.component";
import { JsonrpcRequest, JsonrpcResponseSuccess } from "src/app/shared/jsonrpc/base";
import { States } from "src/app/shared/ngrx-store/states";

export namespace GeoCodeJsonRpc {

    export class Request extends JsonrpcRequest {
        private static METHOD: string = "geocode";
        protected override requiredState: States = States.EDGE_SELECTED;

        public constructor(
            public override params: {
                query: string
            }
        ) {
            super(Request.METHOD, params);
        }
    }

    export class Response extends JsonrpcResponseSuccess {

        public constructor(
            public override readonly id: string,
            public override readonly result: {
                geocodingResults: GeoResult[],
            },
        ) {
            super(id, result);
        }

    }
}
