import { Injectable } from "@angular/core";
import { JsonrpcRequest } from "../../jsonrpc/base";
import { JsonRpcUtils } from "../../jsonrpc/jsonrpcutils";
import { EdgeRpcRequest } from "../../jsonrpc/request/edgeRpcRequest";
import { States } from "../../ngrx-store/states";
import { Edge } from "../../shared";
import { AuthService } from "../auth/auth.service";
import { Pagination } from "../pagination";
import { Websocket } from "../websocket";


@Injectable({ providedIn: "root" })
export class JsonrpcRequestStateHandler {

    public async establishRequestMinState(request: JsonrpcRequest, websocket: Websocket): Promise<void> {
        // eslint-disable-next-line no-async-promise-executor
        return new Promise<void>(async (resolve) => {
            const authService = websocket.injector.get(AuthService);
            const pagination = websocket.injector.get(Pagination);
            const mostInnerReq = JsonRpcUtils.getMostInnerRequest(request);

            if (mostInnerReq == null) {
                resolve();
                return;
            }

            if (States.isAtLeast(mostInnerReq.RequiredState, States.WEBSOCKET_CONNECTED)) {
                await websocket.reconnectIfNeeded();
            }

            if (States.isAtLeast(mostInnerReq.RequiredState, States.AUTHENTICATED)) {
                await authService.authenticate(websocket);
            }

            const req = request as EdgeRpcRequest;
            const edgeId = req.params.edgeId ?? null;
            let edge: Edge | null = null;
            if (edgeId == null) {
                resolve();
                return;
            }

            if (States.isAtLeast(mostInnerReq.RequiredState, States.EDGE_SELECTED)) {
                edge = await pagination.getEdge(edgeId, websocket);
            }

            if (States.isAtLeast(mostInnerReq.RequiredState, States.EDGE_SUBSCRIBED)) {
                await pagination.subscribeEdge(edge, websocket);
            }

            resolve();
        });
    }
}
