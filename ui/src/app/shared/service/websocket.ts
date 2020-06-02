import { AuthenticateWithSessionIdFailedNotification } from '../jsonrpc/notification/authenticatedWithSessionIdFailedNotification';
import { AuthenticateWithSessionIdNotification } from '../jsonrpc/notification/authenticatedWithSessionIdNotification';
import { BehaviorSubject, Subject } from 'rxjs';
import { CurrentDataNotification } from '../jsonrpc/notification/currentDataNotification';
import { DefaultTypes } from './defaulttypes';
import { delay, retryWhen } from 'rxjs/operators';
import { EdgeConfigNotification } from '../jsonrpc/notification/edgeConfigNotification';
import { EdgeRpcNotification } from '../jsonrpc/notification/edgeRpcNotification';
import { environment as env } from '../../../environments';
import { Injectable } from '@angular/core';
import { JsonrpcMessage, JsonrpcNotification, JsonrpcRequest, JsonrpcResponse, JsonrpcResponseError, JsonrpcResponseSuccess } from '../jsonrpc/base';
import { Router } from '@angular/router';
import { Service } from './service';
import { SubscribeSystemLogRequest } from '../jsonrpc/request/subscribeSystemLogRequest';
import { SystemLogNotification } from '../jsonrpc/notification/systemLogNotification';
import { TranslateService } from '@ngx-translate/core';
import { webSocket, WebSocketSubject } from 'rxjs/webSocket';
import { WsData } from './wsdata';
import { UpdateDataNotification } from '../jsonrpc/notification/updateDataNotification';

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
    private translate: TranslateService
  ) {
    service.websocket = this;
    if (env.backend != 'App') {
      setTimeout(() => {
        this.connect();
      })
    }
    // try to auto connect using token or session_id

  }

  /**
   * Opens a connection using a stored token or a cookie with a session_id for this websocket. Called once by constructor
   */
  private connect(): BehaviorSubject<boolean> {
    if (this.socket != null) {
      return this.isWebsocketConnected;
    }
    if (env.backend !== 'OpenEMS Edge') {
      this.service.showLoader();
    }


    if (env.debugMode) {
      console.info("Websocket connect to URL [" + env.url + "]");
    }

    this.socket = webSocket({
      url: this.getUrl(),
      openObserver: {
        next: (value) => {
          if (env.debugMode) {
            console.info("Websocket connection opened");
          }
          this.isWebsocketConnected.next(true);
          if (this.status == 'online') {
            //resubscribes if websocket is reestablished and current view is live or history
            if (this.router.url.split("/")[this.router.url.split("/").length - 1] == "live" ||
              this.router.url.split("/")[this.router.url.split("/").length - 1] == "history") {
              this.service.getCurrentEdge().then(edge => {
                if (edge != null) {
                  edge.sendSubscribeChannels(this);
                }
              })
            }
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
          this.service.notify({
            message: this.translate.instant('General.connectionLost'), // TODO translate
            type: 'warning'
          });
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
        if (env.backend !== 'OpenEMS Edge') {
          this.service.hideLoader();
        }
        if (env.backend === 'App') {
          this.service.spinnerDialog.hide();
        }
        break;

      case AuthenticateWithSessionIdFailedNotification.METHOD:
        this.handleAuthenticateWithSessionIdFailed(message as AuthenticateWithSessionIdNotification);
        if (env.backend !== 'OpenEMS Edge') {
          this.service.hideLoader();
        }
        if (env.backend === 'App') {
          this.service.spinnerDialog.hide();
        }
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
        window.location.href = "/primus-online-monitoring/";
      } else {
        console.info("would redirect...");
      }
    } else if (env.backend === "OpenEMS Edge" || env.backend === "App") {
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

      case UpdateDataNotification.METHOD:
        this.handleUpdateDataNotification(edgeId, message as UpdateDataNotification);
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

  private handleUpdateDataNotification(edgeId: string, message: UpdateDataNotification): void {
    let edges = this.service.edges.getValue();

    if (edgeId in edges) {
      let edge = edges[edgeId];
      edge.handleUpdateDataNotification(message);
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
      this.sendRequest(new SubscribeSystemLogRequest({ subscribe: false }));
    }
  }

  public wpconnect() {
    this.connect();

  }

  getUrl(): string {
    if (env.backend === 'App') {
      return env.url + "?auth=" + this.service.getAuth();
    }
    return env.url;
  }

  public logOut() {
    this.router.navigate(['/']).then(() => {
      this.socket = null;
      this.status = "waiting for authentication";
      this.service.removeToken();

    });
  }

}
