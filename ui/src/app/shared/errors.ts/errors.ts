import { JsonrpcRequest } from "../jsonrpc/base";

export class UnimplementedError<T extends JsonrpcRequest> extends Error {
    constructor(request: T, msg?: string) {

        super(request.constructor.name);
        if (msg) {
            this.message = this.message + " " + msg;
        }
    }
}

export class UnimplementedInEdgeError<T extends JsonrpcRequest> extends UnimplementedError<T> {
    constructor(request: T) {
        super(request, "not available with edge as backend");
    }
}
