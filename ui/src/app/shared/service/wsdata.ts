import { JsonrpcResponse, JsonrpcRequest, JsonrpcNotification } from "./jsonrpc/base";
import { WebSocketSubject } from "rxjs/webSocket";

import { environment as env } from '../../../environments';

export class WsData {

  // Holds callbacks for JSON-RPC Requests. Id is a UUID.
  private callbacks: { [id: string]: (response: JsonrpcResponse) => void } = {};

  /**
   * Sends a JSON-RPC request to a Websocket and registers a callback.
   * 
   * @param ws 
   * @param request 
   * @param responseCallback 
   */
  public sendRequest(ws: WebSocketSubject<any>, request: JsonrpcRequest, responseCallback: (response: JsonrpcResponse) => void) {
    if (env.debugMode) {
      console.info("SEND Request: ", request);
    }
    if (request.id in this.callbacks) {
      // TODO create Error class
      responseCallback.apply({
        id: request.id
      });

    } else {
      this.callbacks[request.id] = responseCallback;
      ws.next(request);
    }
  }

  /**
   * Sends a JSON-RPC notification to a Websocket.
   * 
   * @param ws 
   * @param notification 
   */
  public sendNotification(ws: WebSocketSubject<any>, notification: JsonrpcNotification) {
    ws.next(notification);
  }

  /**
   * Handles a JSON-RPC response by calling the previously registers request callback.
   * 
   * @param response 
   */
  public handleJsonrpcResponse(response: JsonrpcResponse) {
    let responseCallback = this.callbacks[response.id];
    if (responseCallback) {
      responseCallback(response);
    } else {
      console.warn("Got Response without Request: " + response);
    }
  }
}