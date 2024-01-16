import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { CookieService } from 'ngx-cookie-service';
import { delay, retryWhen } from 'rxjs/operators';
import { webSocket, WebSocketSubject } from 'rxjs/webSocket';
import { environment } from "src/environments";

import { JsonrpcMessage, JsonrpcNotification, JsonrpcRequest, JsonrpcResponse, JsonrpcResponseError, JsonrpcResponseSuccess } from '../jsonrpc/base';
import { CurrentDataNotification } from '../jsonrpc/notification/currentDataNotification';
import { EdgeConfigNotification } from '../jsonrpc/notification/edgeConfigNotification';
import { EdgeRpcNotification } from '../jsonrpc/notification/edgeRpcNotification';
import { SystemLogNotification } from '../jsonrpc/notification/systemLogNotification';
import { AuthenticateWithPasswordRequest } from '../jsonrpc/request/authenticateWithPasswordRequest';
import { AuthenticateWithTokenRequest } from '../jsonrpc/request/authenticateWithTokenRequest';
import { EdgeRpcRequest } from '../jsonrpc/request/edgeRpcRequest';
import { LogoutRequest } from '../jsonrpc/request/logoutRequest';
import { RegisterUserRequest } from '../jsonrpc/request/registerUserRequest';
import { AuthenticateResponse } from '../jsonrpc/response/authenticateResponse';
import { Language } from '../type/language';
import { Pagination } from './pagination';
import { Service } from './service';
import { WebsocketInterface } from './websocketInterface';
import { WsData } from './wsdata';

@Injectable()
export class Websocket implements WebsocketInterface {
  private static readonly DEFAULT_EDGEID = 0;
  private readonly wsdata = new WsData();

  private socket: WebSocketSubject<any>;

  public status:
    'initial' // before first connection attempt
    | 'connecting' // trying to connect to backend
    | 'authenticating' // sent authentication request; waiting for response
    | 'waiting for credentials' // login is required. Waiting for credentials input
    | 'online' // logged in + normal operation
    | 'failed' // connection failed
    = 'initial';

  constructor(
    private service: Service,
    private translate: TranslateService,
    private cookieService: CookieService,
    private router: Router,
    private pagination: Pagination,
  ) {
    service.websocket = this;

    // try to auto connect using token or session_id
    setTimeout(() => {
      this.connect();
    });
  }

  /**
   * Opens a connection using a stored token. Called once by constructor
   */
  private connect() {
    if (this.status != 'initial') {
      return;
    }
    // trying to connect
    this.status = 'connecting';

    if (environment.debugMode) {
      console.info("Websocket connecting to URL [" + environment.url + "]");
    }

    /*
    * Open Websocket connection + define onOpen/onClose callbacks.
    */
    this.socket = webSocket({
      url: environment.url,
      openObserver: {
        next: (value) => {
          // Websocket connection is open
          if (environment.debugMode) {
            console.info("Websocket connection opened");
          }
          let token = this.cookieService.get('token');
          if (token) {
            // Login with Session Token
            this.login(new AuthenticateWithTokenRequest({ token: token }));
            this.status = 'authenticating';

          } else {
            // No Token -> directly ask for Login credentials
            this.status = 'waiting for credentials';
            this.router.navigate(['/login']);
          }
        },
      },
      closeObserver: {
        next: (value) => {
          // Websocket connection is closed. Auto-Reconnect starts.
          if (environment.debugMode) {
            console.info("Websocket connection closed");
          }
          // trying to connect
          this.status = 'connecting';
        },
      },
    });

    this.socket.pipe(
      // Websocket Auto-Reconnect
      retryWhen((errors) => {
        console.warn(errors);
        return errors.pipe(delay(1000));
      }),

    ).subscribe(originalMessage => {
      // Receive message from server
      let message: JsonrpcRequest | JsonrpcNotification | JsonrpcResponseSuccess | JsonrpcResponseError =
        JsonrpcMessage.from(originalMessage);

      if (message instanceof JsonrpcRequest) {
        // handle JSON-RPC Request
        if (environment.debugMode) {
          console.info("Receive Request", message);
        }
        this.onRequest(message);

      } else if (message instanceof JsonrpcResponse) {
        // handle JSON-RPC Response
        this.wsdata.handleJsonrpcResponse(message);

      } else if (message instanceof JsonrpcNotification) {
        // handle JSON-RPC Notification
        if (environment.debugMode) {
          if (message.method == EdgeRpcNotification.METHOD && 'payload' in message.params) {
            const m = message as EdgeRpcNotification;
            const payload = m.params.payload;
            console.info("Notification [" + m.params.edgeId + "] [" + payload["method"] + "]", payload['params']);
          } else {
            console.info("Notification [" + message.method + "]", message.params);
          }
        }
        this.onNotification(message);
      }

    }, error => {
      this.onError(error);

    }, () => {
      this.status = 'failed';
      this.onClose();
    });
  }

  /**
   * Logs in by sending an authentication JSON-RPC Request and handles the AuthenticateResponse.
   *
   * @param request the JSON-RPC Request
   * @param lang provided for @demo User. This doesn't change the global language, its just set locally
   */
  public login(request: AuthenticateWithPasswordRequest | AuthenticateWithTokenRequest): Promise<void> {
    return new Promise<void>((resolve) => {
      this.sendRequest(request).then(r => {
        let authenticateResponse = (r as AuthenticateResponse).result;

        let language = Language.getByKey(localStorage.DEMO_LANGUAGE ?? authenticateResponse.user.language.toLocaleLowerCase());
        localStorage.LANGUAGE = language.key;
        this.service.setLang(language);
        this.status = 'online';

        // received login token -> save in cookie
        this.cookieService.set('token', authenticateResponse.token, { expires: 365, path: '/', sameSite: 'Strict' });

        this.service.currentUser = authenticateResponse.user;

        // Metadata
        this.service.metadata.next({
          user: authenticateResponse.user,
          edges: {},
        });

        // Resubscribe Channels
        this.service.getCurrentEdge().then(edge => {

          this.pagination.getAndSubscribeEdge(edge).then(() => {
            edge.subscribeChannelsSuccessful = true;
            if (edge != null) {
              edge.subscribeChannelsOnReconnect(this);
            }
          });
        });

        this.router.initialNavigation();
        resolve();
      }).catch(reason => {
        this.checkErrorCode(reason);
        resolve();
      });
    });
  }

  private checkErrorCode(reason: JsonrpcResponseError) {

    // TODO create global Errorhandler for any type of error
    switch (reason.error.code) {
      case 1003:
        this.service.toast(this.translate.instant('Login.authenticationFailed'), 'danger');
        this.onLoggedOut();
        break;
      case 1:
        this.service.toast(this.translate.instant("Login.REQUEST_TIMEOUT"), "danger");
        this.status = 'waiting for credentials';
        this.service.onLogout();
        break;
      default:
        this.onLoggedOut();
    }
  }

  /**
   * Logs out by sending a logout JSON-RPC Request.
   */
  public logout() {
    this.sendRequest(new LogoutRequest()).then(response => {
      this.onLoggedOut();
    }).catch(reason => {
      console.error(reason);
    });
  }

  private onLoggedOut(): void {
    this.status = 'waiting for credentials';
    this.cookieService.delete('token', '/');
    this.service.onLogout();
  }

  /**
   * Sends a JSON-RPC Request to a Websocket and promises a callback.
   *
   * @param request the JSON-RPC Request
   */
  public sendRequest(request: JsonrpcRequest): Promise<JsonrpcResponseSuccess> {
    if (
      // logged in + normal operation
      this.status == 'online'
      // otherwise only authentication request allowed
      || (request instanceof AuthenticateWithPasswordRequest || request instanceof AuthenticateWithTokenRequest || request instanceof RegisterUserRequest)) {

      return new Promise((resolve, reject) => {
        this.wsdata.sendRequest(this.socket, request).then(response => {
          if (environment.debugMode) {
            if (request instanceof EdgeRpcRequest) {
              console.info("Response     [" + request.params.payload.method + ":" + request.params.edgeId + "]", response.result['payload']['result']);
            } else {
              console.info("Response     [" + request.method + "]", response.result);
            }
          }
          resolve(response);

        }).catch(reason => {
          if (environment.debugMode) {
            if (reason instanceof JsonrpcResponseError) {
              console.warn("Request failed [" + request.method + "]", reason.error);

              if (request instanceof EdgeRpcRequest && reason.error?.code == 3000 /* Edge is not connected */) {
                let edges = this.service.metadata.value?.edges ?? {};
                if (request.params.edgeId in edges) {
                  edges[request.params.edgeId].isOnline = false;
                }
              }
            } else {
              console.warn("Request failed [" + request.method + "]", reason);
            }
          }
          reject(reason);

        });
      });

    } else {
      return Promise.reject("Websocket is not connected or authenticated! Unable to send Request: " + JSON.stringify(request));
    }
  }

  /**
     * Waits until Websocket is 'online' and then
     * sends a safe JSON-RPC Request to a Websocket and promises a callback.
     *
     * @param request the JSON-RPC Request
     */
  public sendSafeRequest(request: JsonrpcRequest): Promise<JsonrpcResponseSuccess> {
    return new Promise<JsonrpcResponseSuccess>((resolve, reject) => {
      let interval = setInterval(() => {

        // TODO: Status should be Observable, furthermore status should be like state-machine
        if (this.status == 'online') {
          clearInterval(interval);
          this.sendRequest(request)
            .then((response) => resolve(response))
            .catch((err) => reject(err));
        }
      }, 500);
    });
  }

  /**
   * Sends a JSON-RPC notification to a Websocket.
   *
   * @param notification the JSON-RPC Notification
   */
  public sendNotification(notification: JsonrpcNotification): void {
    if (this.status != 'online') {
      console.warn("Websocket is not connected! Unable to send Notification", notification);
    }
    this.wsdata.sendNotification(this.socket, notification);
  }

  /**
   * Handle new JSON-RPC Request
   *
   * @param message the JSON-RPC Request
   */
  private onRequest(message: JsonrpcRequest): void {
    console.warn("Unhandled Request: " + message);
  }

  /**
   * Handle new JSON-RPC Notification
   *
   * @param message the JSON-RPC Notification
   */
  private onNotification(message: JsonrpcNotification): void {
    switch (message.method) {
      case EdgeRpcNotification.METHOD:
        this.handleEdgeRpcNotification(message as EdgeRpcNotification);
        break;
      default:
        console.warn("Unhandled Notification: " + message);
    }
  }

  /**
   * Handle Websocket error.
   *
   * @param error the error
   */
  private onError(error: any): void {
    console.error("Websocket error", error);
  }

  /**
   * Handle Websocket closed event.
   */
  private onClose(): void {
    console.info("Websocket closed.");
  }

  /**
   * Handles an EdgeRpcNotification.
   *
   * @param message the EdgeRpcNotification
   */
  private handleEdgeRpcNotification(edgeRpcNotification: EdgeRpcNotification): void {
    let edgeId = edgeRpcNotification.params.edgeId;
    let message = edgeRpcNotification.params.payload;

    let edges = this.service.metadata.value?.edges ?? {};
    if (edgeId in edges) {
      let edge = edges[edgeId];

      switch (message.method) {
        case EdgeConfigNotification.METHOD:
          edge.isOnline = true; // Mark Edge as online
          edge.handleEdgeConfigNotification(message as EdgeConfigNotification);
          break;

        case CurrentDataNotification.METHOD:
          edge.handleCurrentDataNotification(message as CurrentDataNotification);
          break;

        case SystemLogNotification.METHOD:
          edge.handleSystemLogNotification(message as SystemLogNotification);
          break;
      }
    }
  }
}
