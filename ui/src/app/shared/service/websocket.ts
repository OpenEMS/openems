import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, Subject } from 'rxjs';
import { delay, retryWhen } from 'rxjs/operators';
import { webSocket, WebSocketSubject } from 'rxjs/webSocket';
import { environment as env } from '../../../environments';
import { JsonrpcMessage, JsonrpcNotification, JsonrpcRequest, JsonrpcResponse, JsonrpcResponseError, JsonrpcResponseSuccess } from '../jsonrpc/base';
import { AuthenticateWithSessionIdFailedNotification } from '../jsonrpc/notification/authenticatedWithSessionIdFailedNotification';
import { AuthenticateWithSessionIdNotification } from '../jsonrpc/notification/authenticatedWithSessionIdNotification';
import { CurrentDataNotification } from '../jsonrpc/notification/currentDataNotification';
import { EdgeRpcNotification } from '../jsonrpc/notification/edgeRpcNotification';
import { EdgeRpcResponse } from '../jsonrpc/response/edgeRpcResponse';
import { DefaultTypes } from './defaulttypes';
import { Service } from './service';
import { WsData } from './wsdata';
import { SystemLogNotification } from '../jsonrpc/notification/systemLogNotification';
import { SubscribeSystemLogRequest } from '../jsonrpc/request/subscribeSystemLogRequest';
import { EdgeConfigNotification } from '../jsonrpc/notification/edgeConfigNotification';

@Injectable()
export class Websocket {
  private static readonly DEFAULT_EDGEID = 0;
  private readonly wsdata = new WsData();

  private socket: WebSocketSubject<any>;
  public status: DefaultTypes.ConnectionStatus = "connecting";
  public isWebsocketConnected: BehaviorSubject<boolean> = new BehaviorSubject(false);

  private username: string = "";
  // private messages: Observable<string>;
  private queryreply = new Subject<{ id: string[] }>();
  private stopOnInitialize: Subject<void> = new Subject<void>();

  // tracks which message id (=key) is connected with which edgeId (=value)
  private pendingQueryReplies: { [id: string]: string } = {};

  constructor(
    private router: Router,
    private service: Service,
  ) {
    service.websocket = this;

    // try to auto connect using token or session_id
    setTimeout(() => {
      this.connect();
    })
  }

  /**
   * Opens a connection using a stored token or a cookie with a session_id for this websocket. Called once by constructor
   */
  private connect(): BehaviorSubject<boolean> {
    if (this.socket != null) {
      return this.isWebsocketConnected;
    }

    if (env.debugMode) {
      console.info("Websocket connect to URL [" + env.url + "]");
    }

    this.socket = webSocket({
      url: env.url,
      openObserver: {
        next: (value) => {
          if (env.debugMode) {
            console.info("Websocket connection opened");
          }
          this.isWebsocketConnected.next(true);
          if (this.status == 'online') {
            this.service.notify({
              message: "Connection lost. Trying to reconnect.", // TODO translate
              type: 'warning'
            });
            // TODO show spinners everywhere
            this.status = 'connecting';
          } else {
            this.status = 'waiting for authentication';
          }
        }
      },
      closeObserver: {
        next: (value) => {
          // TODO: reconnect
          if (env.debugMode) {
            console.info("Websocket connection closed");
          }
          this.isWebsocketConnected.next(false);
        }
      }
    });

    this.socket.pipe(
      retryWhen(errors => {
        console.warn("Websocket was interrupted. Retrying in 2 seconds.");
        return errors.pipe(delay(2000));

      })).subscribe(originalMessage => {
        // called on every receive of message from server
        let message: JsonrpcRequest | JsonrpcNotification | JsonrpcResponseSuccess | JsonrpcResponseError;
        try {
          message = JsonrpcMessage.from(originalMessage);
        } catch (e) {
          // handle deprecated non-JSON-RPC messages
          if (env.debugMode) {
            console.info("Convert non-JSON-RPC message", message);
          }
          message = this.handleNonJsonrpcMessage(originalMessage, e);
        }

        if (message instanceof JsonrpcRequest) {
          // handle JSON-RPC Request
          if (env.debugMode) {
            console.info("Receive Request", message);
          }
          this.onRequest(message);

        } else if (message instanceof JsonrpcResponse) {
          // handle JSON-RPC Response
          this.onResponse(message);

        } else if (message instanceof JsonrpcNotification) {
          // handle JSON-RPC Notification
          if (env.debugMode) {
            if (message.method == EdgeRpcNotification.METHOD && 'payload' in message.params) {
              const payload = message.params['payload'];
              console.info("Notification [" + payload["method"] + "]", payload);
            } else {
              console.info("Notification [" + message.method + "]", message);
            }
          }
          this.onNotification(message);
        }
      }, error => {
        this.onError(error);

      }, () => {
        this.onClose();

      })
    return this.isWebsocketConnected;
  }

  /**
   * Sends a JSON-RPC request to a Websocket and registers a callback.
   * 
   * @param request 
   * @param responseCallback 
   */
  public sendRequest(request: JsonrpcRequest): Promise<JsonrpcResponseSuccess> {
    if (!this.isWebsocketConnected.value) {
      return Promise.reject("Websocket is not connected! Unable to send Request: " + JSON.stringify(request));
    }
    return this.wsdata.sendRequest(this.socket, request);
  }

  /**
   * Sends a JSON-RPC notification to a Websocket.
   * 
   * @param notification 
   */
  public sendNotification(notification: JsonrpcNotification): void {
    if (!this.isWebsocketConnected.value) {
      console.warn("Websocket is not connected! Unable to send Notification", notification);
    }
    this.wsdata.sendNotification(this.socket, notification);
  }

  /**
   * Handle deprecated non-JSON-RPC message
   * 
   * @param originalMessage 
   * @param e 
   */
  private handleNonJsonrpcMessage(originalMessage: any, e: any): JsonrpcRequest | JsonrpcNotification | JsonrpcResponseSuccess | JsonrpcResponseError {
    throw new Error("Unhandled Non-JSON-RPC message: " + e);
  }

  /**
   * Handle new JSON-RPC Request
   * 
   * @param message 
   * @param responseCallback 
   */
  private onRequest(message: JsonrpcRequest): void {
    // responseCallback.apply(...)
    console.log("On Request: " + message);
  }

  /**
   * Handle new JSON-RPC Response
   * 
   * @param message 
   */
  private onResponse(response: JsonrpcResponse): void {
    this.wsdata.handleJsonrpcResponse(response);
  }

  /**
   * Handle new JSON-RPC Notification
   * 
   * @param message 
   */
  private onNotification(message: JsonrpcNotification): void {
    switch (message.method) {
      case AuthenticateWithSessionIdNotification.METHOD:
        this.handleAuthenticateWithSessionId(message as AuthenticateWithSessionIdNotification);
        break;

      case AuthenticateWithSessionIdFailedNotification.METHOD:
        this.handleAuthenticateWithSessionIdFailed(message as AuthenticateWithSessionIdNotification);
        break;

      case EdgeRpcNotification.METHOD:
        this.handleEdgeRpcNotification(message as EdgeRpcNotification);
        break;
    }
  }

  /**
   * Handle Websocket error.
   * 
   * @param error
   */
  private onError(error: any): void {
    console.error("Websocket error", error);
  }

  /**
   * Handle Websocket closed event.
   */
  private onClose(): void {
    console.info("Websocket closed.");
    // TODO: reconnect
  }

  /**
   * Handles a AuthenticateWithSessionIdNotification.
   * 
   * @param message 
   */
  private handleAuthenticateWithSessionId(message: AuthenticateWithSessionIdNotification): void {
    this.service.handleAuthentication(message.params.token, message.params.edges);
  }

  /**
   * Handles a AuthenticateWithSessionIdFailedNotification.
   * 
   * @param message 
   */
  private handleAuthenticateWithSessionIdFailed(message: AuthenticateWithSessionIdFailedNotification): void {
    if (env.backend === "OpenEMS Backend") {
      if (env.production) {
        window.location.href = "/web/login?redirect=/m/index";
      } else {
        console.info("would redirect...");
      }
    } else if (env.backend === "OpenEMS Edge") {
      this.router.navigate(['/index']);
    }
  }

  /**
   * Handles an EdgeRpcNotification.
   * 
   * @param message 
   */
  private handleEdgeRpcNotification(edgeRpcNotification: EdgeRpcNotification): void {
    let edgeId = edgeRpcNotification.params.edgeId;
    let message = edgeRpcNotification.params.payload;

    switch (message.method) {
      case EdgeConfigNotification.METHOD:
        this.handleEdgeConfigNotification(edgeId, message as EdgeConfigNotification);
        break;

      case CurrentDataNotification.METHOD:
        this.handleCurrentDataNotification(edgeId, message as CurrentDataNotification);
        break;

      case SystemLogNotification.METHOD:
        this.handleSystemLogNotification(edgeId, message as SystemLogNotification);
        break;
    }
  }

  /**
   * Handles a EdgeConfigNotification.
   * 
   * @param message 
   */
  private handleEdgeConfigNotification(edgeId: string, message: EdgeConfigNotification): void {
    let edges = this.service.edges.getValue();

    if (edgeId in edges) {
      let edge = edges[edgeId];
      edge.handleEdgeConfigNotification(message);
    }
  }

  /**
   * Handles a CurrentDataNotification.
   * 
   * @param message 
   */
  private handleCurrentDataNotification(edgeId: string, message: CurrentDataNotification): void {
    let edges = this.service.edges.getValue();

    if (edgeId in edges) {
      let edge = edges[edgeId];
      edge.handleCurrentDataNotification(message);
    }
  }

  /**
   * Handles a SystemLogNotification.
   * 
   * @param message 
   */
  private handleSystemLogNotification(edgeId: string, message: SystemLogNotification): void {
    let edges = this.service.edges.getValue();

    if (edgeId in edges) {
      let edge = edges[edgeId];
      edge.handleSystemLogNotification(message);
    } else {
      this.sendRequest(new SubscribeSystemLogRequest(false));
    }
  }

}
