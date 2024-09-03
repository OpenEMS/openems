import { JsonrpcNotification, JsonrpcRequest, JsonrpcResponseSuccess } from "../../jsonrpc/base";
import { AuthenticateWithPasswordRequest } from "../../jsonrpc/request/authenticateWithPasswordRequest";
import { AuthenticateWithTokenRequest } from "../../jsonrpc/request/authenticateWithTokenRequest";
import { WebsocketInterface } from "../websocketInterface";

export class DummyWebsocket implements WebsocketInterface {

    public login(request: AuthenticateWithPasswordRequest | AuthenticateWithTokenRequest) {
        throw new Error("Method not implemented.");
    }

    public logout(): void {
        throw new Error("Method not implemented.");
    }

    public sendRequest(request: JsonrpcRequest): Promise<JsonrpcResponseSuccess> {
        return new Promise((accept, reject) => {
            reject("DummyComponent");
        });
    }

    public sendNotification(notification: JsonrpcNotification): void {
        throw new Error("Method not implemented.");
    }

}
