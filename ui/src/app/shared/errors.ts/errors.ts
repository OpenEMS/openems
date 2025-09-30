import { JsonrpcRequest } from "../jsonrpc/base";

export class UnimplementedError<T extends JsonrpcRequest> extends Error {
    constructor(request: T, msg?: string) {

        super(REQUEST.CONSTRUCTOR.NAME);
        if (msg) {
            THIS.MESSAGE = THIS.MESSAGE + " " + msg;
        }
    }
}

export class UnimplementedInEdgeError<T extends JsonrpcRequest> extends UnimplementedError<T> {
    constructor(request: T) {
        super(request, "not available with edge as backend");
    }
}
