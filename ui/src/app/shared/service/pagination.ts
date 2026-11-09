// @ts-strict-ignore
import { Directive, inject } from "@angular/core";
import { Router } from "@angular/router";
import { JsonRpcUtils } from "../jsonrpc/jsonrpcutils";
import { SubscribeEdgesRequest } from "../jsonrpc/request/subscribeEdgesRequest";
import { Edge, Websocket } from "../shared";
import { States } from "../states/states";
import { Service } from "./service";

@Directive()
export class Pagination {
    private readonly service = inject(Service);
    private readonly router = inject(Router);

    public getAndSubscribeEdge(edgeId: Edge["id"]): Promise<void> {
        return new Promise<void>((resolve, reject) => {
            this.getEdge(edgeId, this.service.websocket)
                .then(async (edge) => {
                    await this.subscribeEdge(edge, this.service.websocket);
                    this.service.websocket.state.set(States.EDGE_SELECTED);
                    resolve();
                })
                .catch(() => {
                    this.router.navigate(["index"]);
                    reject();
                });
        });
    }

    public async getEdge(edgeId: Edge["id"], websocket: Websocket): Promise<Edge> {
        const edge = this.service.currentEdge();
        if (States.isAtLeast(websocket.state(), States.EDGE_SELECTED) && edge != null) {
            return Promise.resolve(edge);
        }
        return new Promise<Edge>((res) => {
            res(this.service.updateCurrentEdge(edgeId));
        });
    }

    public async subscribeEdge(edge: Edge | null, websocket: Websocket): Promise<void> {
        // TODO: Remove when only new ui design exists, this component is initialiazed twice due to rendering issues, problem exists because of app.component.html
        if (States.isAtLeast(websocket.state(), States.EDGE_SUBSCRIBED)) {
            return;
        }

        if (!edge) {
            throw new Error("Cannot subscribe to an edge: no edge selected.");
        }

        const req = new SubscribeEdgesRequest({ edges: [edge.id] });
        const [err, _result] = await JsonRpcUtils.handle(this.service.websocket.sendStateFullRequest(req));

        if (err) {
            throw err;
        }

        this.service.websocket.state.set(States.EDGE_SUBSCRIBED);
        return Promise.resolve();
    }
}
