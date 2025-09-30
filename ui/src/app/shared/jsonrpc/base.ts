import { v4 as uuidv4 } from "uuid";

export abstract class JsonrpcMessage {
    public readonly jsonrpc: string = "2.0";

    protected constructor(
    ) { }

    public static from(message: any): JsonrpcRequest | JsonrpcNotification | JsonrpcResponseSuccess | JsonrpcResponseError {
        if ("method" in message && "params" in message) {
            if ("id" in message) {
                return new JsonrpcRequest(MESSAGE.ID, MESSAGE.METHOD, MESSAGE.PARAMS);
            } else {
                return new JsonrpcNotification(MESSAGE.METHOD, MESSAGE.PARAMS);
            }
        } else if ("result" in message) {
            return new JsonrpcResponseSuccess(MESSAGE.ID, MESSAGE.RESULT);
        } else if ("error" in message) {
            return new JsonrpcResponseError(MESSAGE.ID, MESSAGE.ERROR);
        } else {
            throw new Error("JsonrpcMessage is not a valid Request, Result or Notification: " + JSON.STRINGIFY(message));
        }
    }


}

export abstract class AbstractJsonrpcRequest extends JsonrpcMessage {
    protected constructor(
        public readonly method: string,
        public readonly params: {},
    ) {
        super();
    }
}

export class JsonrpcRequest extends AbstractJsonrpcRequest {
    public constructor(
        public override readonly method: string,
        public override readonly params: {},
        public readonly id: string = uuidv4(),
    ) {
        super(method, params);
    }
}

export class JsonrpcNotification extends AbstractJsonrpcRequest {
    public constructor(
        public override readonly method: string,
        public override readonly params: {},
    ) {
        super(method, params);
    }
}

export abstract class JsonrpcResponse extends JsonrpcMessage {
    public constructor(
        public readonly id: string,
    ) {
        super();
    }
}

export class JsonrpcResponseSuccess extends JsonrpcResponse {
    public constructor(
        public override readonly id: string,
        public readonly result: {},
    ) {
        super(id);
    }
}

export class JsonrpcResponseError extends JsonrpcResponse {
    public constructor(
        public override readonly id: string,
        public readonly error: {
            code: number,
            message: string,
            data?: {}
        },
    ) {
        super(id);
    }
}
