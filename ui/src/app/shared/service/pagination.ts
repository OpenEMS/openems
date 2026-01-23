// @ts-strict-ignore
import { Directive } from "@angular/core";
import { Router } from "@angular/router";
import { JsonRpcUtils } from "../jsonrpc/jsonrpcutils";
import { SubscribeEdgesRequest } from "../jsonrpc/request/subscribeEdgesRequest";
import { States } from "../ngrx-store/states";
import { ChannelAddress, Edge } from "../shared";
import { Service } from "./service";

@Directive()
export class Pagination {

    private edge: Edge | null = null;
    private count = 0;

    constructor(
        public service: Service,
        private router: Router,
    ) { }

    public getAndSubscribeEdge(edge: Edge): Promise<void> {
        return new Promise<void>((resolve) => {
            this.service.updateCurrentEdge(edge.id).then((edge) => {

                this.edge = edge;
                this.subscribeEdge(edge);
            }).then(() => {
                this.service.websocket.state.set(States.EDGE_SELECTED);
                this.edge.subscribeChannels(this.service.websocket, "", [
                    new ChannelAddress("_sum", "State"),
                ]);
            })
                .finally(resolve)
                .catch(() => {
                    this.router.navigate(["index"]);
                });
        });
    }

    public async subscribeEdge(edge: Edge) {

        if (!edge) {
            return;
        }

        const [err, _result] = await JsonRpcUtils.handle(this.service.websocket.sendRequest(new SubscribeEdgesRequest({ edges: [edge.id] })));

        if (err) {
            throw err;
        }
    }
}
