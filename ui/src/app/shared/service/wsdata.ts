import { EdgeRpcRequest } from "../jsonrpc/request/edgeRpcRequest";
import { environment as env } from '../../../environments';
import { JsonrpcNotification, JsonrpcRequest, JsonrpcResponse, JsonrpcResponseError, JsonrpcResponseSuccess } from "../jsonrpc/base";
import { WebSocketSubject } from "rxjs/webSocket";

export class WsData {

  // Holds Promises for JSON-RPC Requests. Id is a UUID.
  private requestPromises: {
    [id: string]: {
      resolve: (value?: JsonrpcResponseSuccess | PromiseLike<JsonrpcResponseSuccess>) => void;
      reject: (reason?: JsonrpcResponseError) => void;
    }
  } = {};

  /**
   * Sends a JSON-RPC request to a Websocket and registers a callback.
   * 
   * @param ws 
   * @param request 
   * @param responseCallback 
   */
  public sendRequest(ws: WebSocketSubject<any>, request: JsonrpcRequest): Promise<JsonrpcResponseSuccess> {
    if (env.debugMode) {
      if (request instanceof EdgeRpcRequest) {
        console.info("Request      [" + request.params.payload.method + ":" + request.params.edgeId + "]", request.params.payload);
      } else {
        console.info("Request      [" + request.method + "]", request);
      }
    }

    // create Promise
    let promiseResolve: (value?: JsonrpcResponseSuccess | PromiseLike<JsonrpcResponseSuccess>) => void;
    let promiseReject: (reason?: any) => void;
    let promise = new Promise<JsonrpcResponseSuccess>((resolve, reject) => {
      promiseResolve = resolve;
      promiseReject = reject;
    });

    // check for existing Promise with this JSON-RPC Request ID
    if (request.id in this.requestPromises) {
      promiseReject("ID already exists"); // TODO JSONRPC_ID_NOT_UNIQUE(4000)

    } else {
      this.requestPromises[request.id] = { resolve: promiseResolve, reject: promiseReject };
      ws.next(request);
    }

    return promise;
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
   * Handles a JSON-RPC response by resolving the previously registered request Promise.
   * 
   * @param response 
   */
  public handleJsonrpcResponse(response: JsonrpcResponse) {
    let promise = this.requestPromises[response.id];
    if (promise) {
      // this was a response on a request
      if (response instanceof JsonrpcResponseSuccess) {
        // Success Response -> resolve Promise
        promise.resolve(response);

      } else if (response instanceof JsonrpcResponseError) {
        // Named OpenEMS-Error Response -> reject Promise
        promise.reject(response);

      } else {
        // Undefined Error Response -> reject Promise
        // TODO use OpenemsError code
        promise.reject(
          new JsonrpcResponseError(response.id, {
            code: 0, message: "Response is neither JsonrpcResponseSuccess nor JsonrpcResponseError: " + response, data: {}
          }));
      }
    } else {
      // TODO throw exception
      console.warn("Got Response without Request", response);
    }
  }
}