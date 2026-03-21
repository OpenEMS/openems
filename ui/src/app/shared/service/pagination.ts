// @ts-strict-ignore
import { Directive } from "@angular/core";
import { Router } from "@angular/router";
import { JsonRpcUtils } from "../jsonrpc/jsonrpcutils";
import { SubscribeEdgesRequest } from "../jsonrpc/request/subscribeEdgesRequest";
import { States } from "../ngrx-store/states";
import { Edge, Websocket } from "../shared";
import { Service } from "./service";

@Directive()
export class Pagination {

    private edge: Edge | null = null;
    private count = 0;

    constructor(
        public service: Service,
        private router: Router,
    ) { }

    public getAndSubscribeEdge(edgeId: Edge["id"]): Promise<void> {
        return new Promise<void>((resolve) => {
            this.getEdge(edgeId, this.service.websocket)
                .then(async (edge) => {
                    this.edge = edge;
                    await this.subscribeEdge(edge, this.service.websocket);
                    this.service.websocket.state.set(States.EDGE_SELECTED);
                })
                .catch(() => {
                    this.router.navigate(["index"]);
                    resolve();
                });
        });
    }

    public async getEdge(edgeId: Edge["id"], websocket: Websocket): Promise<Edge> {

        if (States.isAtLeast(websocket.state(), States.EDGE_SELECTED)) {
            Promise.resolve(this.service.currentEdge());
            return;
        }
        return new Promise<Edge>((res) => {
            res(this.service.updateCurrentEdge(edgeId));
        });
    }

    public async subscribeEdge(edge: Edge | null, websocket: Websocket): Promise<void> {


        if (States.isAtLeast(websocket.state(), States.EDGE_SUBSCRIBED)) {
            Promise.resolve();
            return;
        }

        if (!edge) {
            return;
        }

        const [err, _result] = await JsonRpcUtils.handle(this.service.websocket.sendStateFullRequest(new SubscribeEdgesRequest({ edges: [edge.id] })));

        if (err) {
            throw err;
        }

        this.service.websocket.state.set(States.EDGE_SUBSCRIBED);
    }
}
