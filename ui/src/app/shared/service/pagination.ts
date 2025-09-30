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
      THIS.SERVICE.UPDATE_CURRENT_EDGE(EDGE.ID).then((edge) => {

        THIS.EDGE = edge;
        THIS.SUBSCRIBE_EDGE(edge);
      }).then(() => {
        THIS.SERVICE.WEBSOCKET.STATE.SET(States.EDGE_SELECTED);
        THIS.EDGE.SUBSCRIBE_CHANNELS(THIS.SERVICE.WEBSOCKET, "", [
          new ChannelAddress("_sum", "State"),
        ]);
      })
        .finally(resolve)
        .catch(() => {
          THIS.ROUTER.NAVIGATE(["index"]);
        });
    });
  }

  public async subscribeEdge(edge: Edge) {

    if (!edge) {
      return;
    }

    const [err, _result] = await JSON_RPC_UTILS.HANDLE(THIS.SERVICE.WEBSOCKET.SEND_REQUEST(new SubscribeEdgesRequest({ edges: [EDGE.ID] })));

    if (err) {
      throw err;
    }
  }
}
