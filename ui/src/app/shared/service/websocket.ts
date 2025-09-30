// @ts-strict-ignore
import { Injectable, signal, WritableSignal } from "@angular/core";
import { Router } from "@angular/router";
import { Capacitor } from "@capacitor/core";
import { TranslateService } from "@ngx-translate/core";
import { SavePassword } from "capacitor-ios-autofill-save-password";
import { CookieService } from "ngx-cookie-service";
import { delay, retryWhen } from "rxjs/operators";
import { webSocket, WebSocketSubject } from "rxjs/webSocket";
import { environment } from "src/environments";

import { WebsocketInterface } from "../interface/websocketInterface";
import { JsonrpcMessage, JsonrpcNotification, JsonrpcRequest, JsonrpcResponse, JsonrpcResponseError, JsonrpcResponseSuccess } from "../jsonrpc/base";
import { CurrentDataNotification } from "../jsonrpc/notification/currentDataNotification";
import { EdgeConfigNotification } from "../jsonrpc/notification/edgeConfigNotification";
import { EdgeRpcNotification } from "../jsonrpc/notification/edgeRpcNotification";
import { SystemLogNotification } from "../jsonrpc/notification/systemLogNotification";
import { AuthenticateWithPasswordRequest } from "../jsonrpc/request/authenticateWithPasswordRequest";
import { AuthenticateWithTokenRequest } from "../jsonrpc/request/authenticateWithTokenRequest";
import { EdgeRpcRequest } from "../jsonrpc/request/edgeRpcRequest";
import { LogoutRequest } from "../jsonrpc/request/logoutRequest";
import { RegisterUserRequest } from "../jsonrpc/request/registerUserRequest";
import { AuthenticateResponse } from "../jsonrpc/response/authenticateResponse";
import { User } from "../jsonrpc/shared";
import { States } from "../ngrx-store/states";
import { Language } from "../type/language";
import { Pagination } from "./pagination";
import { Service } from "./service";
import { UserService } from "./USER.SERVICE";
import { WsData } from "./wsdata";

@Injectable()
export class Websocket implements WebsocketInterface {

  public static readonly REQUEST_TIMEOUT = 500;

  private static readonly DEFAULT_EDGEID = 0;

  public status:
    "initial" // before first connection attempt
    | "connecting" // trying to connect to backend
    | "authenticating" // sent authentication request; waiting for response
    | "waiting for credentials" // login is required. Waiting for credentials input
    | "online" // logged in + normal operation
    | "failed" // connection failed
    = "initial";

  public readonly state: WritableSignal<States> = signal(States.WEBSOCKET_NOT_YET_CONNECTED);

  private readonly wsdata = new WsData();

  private socket: WebSocketSubject<any>;

  constructor(
    private service: Service,
    private translate: TranslateService,
    private cookieService: CookieService,
    private router: Router,
    private userService: UserService,
    private pagination: Pagination,
  ) {
    SERVICE.WEBSOCKET = this;

    // try to auto connect using token or session_id
    setTimeout(() => {
      THIS.CONNECT();
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
      THIS.SEND_REQUEST(request).then(r => {
        THIS.STATE.SET(STATES.AUTHENTICATED);
        const authenticateResponse = (r as AuthenticateResponse).result;

        if (request instanceof AuthenticateWithPasswordRequest) {
          if (CAPACITOR.GET_PLATFORM() === "ios") {
            SAVE_PASSWORD.PROMPT_DIALOG({
              username: REQUEST.PARAMS.USERNAME,
              password: REQUEST.PARAMS.PASSWORD,
            });
          }
        }

        const language = LANGUAGE.GET_BY_KEY(localStorage.DEMO_LANGUAGE ?? AUTHENTICATE_RESPONSE.USER.LANGUAGE.TO_LOCALE_LOWER_CASE());
        LOCAL_STORAGE.LANGUAGE = LANGUAGE.KEY;
        THIS.SERVICE.SET_LANG(language);
        THIS.STATUS = "online";

        // received login token -> save in cookie
        THIS.COOKIE_SERVICE.SET("token", AUTHENTICATE_RESPONSE.TOKEN, { expires: 365, path: "/", sameSite: "Strict", secure: LOCATION.PROTOCOL === "https:" });
        THIS.USER_SERVICE.CURRENT_USER.SET(USER.FROM(AUTHENTICATE_RESPONSE.USER));
        // Metadata
        THIS.SERVICE.METADATA.NEXT({
          user: AUTHENTICATE_RESPONSE.USER,
          edges: {},
        });

        // Resubscribe Channels
        THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {

          THIS.PAGINATION.GET_AND_SUBSCRIBE_EDGE(edge).then(() => {
            EDGE.SUBSCRIBE_CHANNELS_SUCCESSFUL = true;
            if (edge != null) {
              EDGE.SUBSCRIBE_CHANNELS_ON_RECONNECT(this);
            }
          });
        });

        THIS.ROUTER.INITIAL_NAVIGATION();
        resolve();
      }).catch(reason => {
        THIS.CHECK_ERROR_CODE(reason);
        resolve();
      });
    });
  }

  /**
   * Logs out by sending a logout JSON-RPC Request.
   */
  public logout() {
    THIS.SEND_REQUEST(new LogoutRequest()).then(response => {
      THIS.ON_LOGGED_OUT();
    }).catch(reason => {
      CONSOLE.ERROR(reason);
    });
  }

  /**
   * Sends a JSON-RPC Request to a Websocket and promises a callback.
   *
   * @param request the JSON-RPC Request
   */
  public sendRequest(request: JsonrpcRequest): Promise<JsonrpcResponseSuccess> {
    if (
      // logged in + normal operation
      THIS.STATUS == "online"
      // otherwise only authentication request allowed
      || (request instanceof AuthenticateWithPasswordRequest || request instanceof AuthenticateWithTokenRequest || request instanceof RegisterUserRequest)) {

      return new Promise((resolve, reject) => {
        THIS.WSDATA.SEND_REQUEST(THIS.SOCKET, request).then(response => {
          if (ENVIRONMENT.DEBUG_MODE) {
            if (request instanceof EdgeRpcRequest) {
              CONSOLE.INFO("Response     [" + REQUEST.PARAMS.PAYLOAD.METHOD + ":" + REQUEST.PARAMS.EDGE_ID + "]", RESPONSE.RESULT["payload"]["result"]);
            } else {
              CONSOLE.INFO("Response     [" + REQUEST.METHOD + "]", RESPONSE.RESULT);
            }
          }
          resolve(response);

        }).catch(reason => {
          if (ENVIRONMENT.DEBUG_MODE) {
            if (reason instanceof JsonrpcResponseError) {
              CONSOLE.WARN("Request failed [" + REQUEST.METHOD + "]", REASON.ERROR);

              if (request instanceof EdgeRpcRequest && REASON.ERROR?.code == 3000 /* Edge is not connected */) {
                const edges = THIS.SERVICE.METADATA.VALUE?.edges ?? {};
                if (REQUEST.PARAMS.EDGE_ID in edges) {
                  edges[REQUEST.PARAMS.EDGE_ID].isOnline = false;
                }
              }
            } else {
              CONSOLE.WARN("Request failed [" + REQUEST.METHOD + "]", reason);
            }
          }
          reject(reason);

        });
      });

    } else {
      return PROMISE.REJECT("Websocket is not connected or authenticated! Unable to send Request: " + JSON.STRINGIFY(request));
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
      const interval = setInterval(() => {

        // TODO: Status should be Observable, furthermore status should be like state-machine
        if (THIS.STATUS == "online") {
          clearInterval(interval);
          THIS.SEND_REQUEST(request)
            .then((response) => resolve(response))
            .catch((err) => reject(err));
        }
      }, Websocket.REQUEST_TIMEOUT);
    });
  }

  /**
   * Sends a JSON-RPC notification to a Websocket.
   *
   * @param notification the JSON-RPC Notification
   */
  public sendNotification(notification: JsonrpcNotification): void {
    if (THIS.STATUS != "online") {
      CONSOLE.WARN("Websocket is not connected! Unable to send Notification", notification);
    }
    THIS.WSDATA.SEND_NOTIFICATION(THIS.SOCKET, notification);
  }

  /**
 * Opens a connection using a stored token. Called once by constructor
 */
  private connect() {
    THIS.STATE.SET(States.WEBSOCKET_NOT_YET_CONNECTED);
    if (THIS.STATUS != "initial") {
      return;
    }
    // trying to connect
    THIS.STATE.SET(States.WEBSOCKET_CONNECTING);
    THIS.STATUS = "connecting";

    if (ENVIRONMENT.DEBUG_MODE) {
      CONSOLE.INFO("Websocket connecting to URL [" + ENVIRONMENT.URL + "]");
    }

    /*
    * Open Websocket connection + define onOpen/onClose callbacks.
    */
    THIS.SOCKET = webSocket({
      url: ENVIRONMENT.URL,
      openObserver: {
        next: (value) => {
          THIS.STATE.SET(States.WEBSOCKET_NOT_YET_CONNECTED);
          // Websocket connection is open
          if (ENVIRONMENT.DEBUG_MODE) {
            CONSOLE.INFO("Websocket connection opened");
          }

          const token = THIS.COOKIE_SERVICE.GET("token");
          if (token) {
            THIS.STATE.SET(States.AUTHENTICATING_WITH_TOKEN);

            // Login with Session Token
            THIS.LOGIN(new AuthenticateWithTokenRequest({ token: token }));
            THIS.STATUS = "authenticating";

          } else {
            // No Token -> directly ask for Login credentials
            THIS.STATE.SET(States.NOT_AUTHENTICATED);
            THIS.STATUS = "waiting for credentials";
            THIS.ROUTER.NAVIGATE(["login"]);
          }
        },
      },
      closeObserver: {
        next: (value) => {
          // Websocket connection is closed. Auto-Reconnect starts.
          THIS.STATE.SET(States.WEBSOCKET_CONNECTION_CLOSED);
          if (ENVIRONMENT.DEBUG_MODE) {
            CONSOLE.INFO("Websocket connection closed");
          }
          // trying to connect
          THIS.STATE.SET(States.WEBSOCKET_CONNECTING);
          THIS.STATUS = "connecting";
        },
      },
    });

    THIS.SOCKET.PIPE(
      // Websocket Auto-Reconnect
      retryWhen((errors) => {
        CONSOLE.WARN(errors);
        return ERRORS.PIPE(delay(1000));
      }),

    ).subscribe(originalMessage => {
      // Receive message from server
      const message: JsonrpcRequest | JsonrpcNotification | JsonrpcResponseSuccess | JsonrpcResponseError =
        JSONRPC_MESSAGE.FROM(originalMessage);

      if (message instanceof JsonrpcRequest) {
        // handle JSON-RPC Request
        if (ENVIRONMENT.DEBUG_MODE) {
          CONSOLE.INFO("Receive Request", message);
        }
        THIS.ON_REQUEST(message);

      } else if (message instanceof JsonrpcResponse) {
        // handle JSON-RPC Response
        THIS.WSDATA.HANDLE_JSONRPC_RESPONSE(message);

      } else if (message instanceof JsonrpcNotification) {
        // handle JSON-RPC Notification
        if (ENVIRONMENT.DEBUG_MODE) {
          if (MESSAGE.METHOD == EDGE_RPC_NOTIFICATION.METHOD && "payload" in MESSAGE.PARAMS) {
            const m = message as EdgeRpcNotification;
            const payload = M.PARAMS.PAYLOAD;
            CONSOLE.INFO("Notification [" + M.PARAMS.EDGE_ID + "] [" + payload["method"] + "]", payload["params"]);
          } else {
            CONSOLE.INFO("Notification [" + MESSAGE.METHOD + "]", MESSAGE.PARAMS);
          }
        }
        THIS.ON_NOTIFICATION(message);
      }

    }, error => {
      THIS.ON_ERROR(error);

    }, () => {
      THIS.STATUS = "failed";
      THIS.ON_CLOSE();
    });
  }

  private checkErrorCode(reason: JsonrpcResponseError) {

    // TODO create global Errorhandler for any type of error
    switch (REASON.ERROR.CODE) {
      case 1003:
        THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("LOGIN.AUTHENTICATION_FAILED"), "danger");
        THIS.ON_LOGGED_OUT();
        break;
      case 1:
        THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("LOGIN.REQUEST_TIMEOUT"), "danger");
        THIS.STATUS = "waiting for credentials";
        THIS.SERVICE.ON_LOGOUT();
        break;
      default:
        THIS.ON_LOGGED_OUT();
    }
  }

  private onLoggedOut(): void {
    THIS.STATUS = "waiting for credentials";
    THIS.COOKIE_SERVICE.DELETE("token", "/");
    THIS.SERVICE.ON_LOGOUT();
  }

  /**
   * Handle new JSON-RPC Request
   *
   * @param message the JSON-RPC Request
   */
  private onRequest(message: JsonrpcRequest): void {
    CONSOLE.WARN("Unhandled Request: " + message);
  }

  /**
   * Handle new JSON-RPC Notification
   *
   * @param message the JSON-RPC Notification
   */
  private onNotification(message: JsonrpcNotification): void {
    switch (MESSAGE.METHOD) {
      case EDGE_RPC_NOTIFICATION.METHOD:
        THIS.HANDLE_EDGE_RPC_NOTIFICATION(message as EdgeRpcNotification);
        break;
      default:
        CONSOLE.WARN("Unhandled Notification: " + message);
    }
  }

  /**
   * Handle Websocket error.
   *
   * @param error the error
   */
  private onError(error: any): void {
    CONSOLE.ERROR("Websocket error", error);
  }

  /**
   * Handle Websocket closed event.
   */
  private onClose(): void {
    CONSOLE.INFO("Websocket closed.");
  }

  /**
   * Handles an EdgeRpcNotification.
   *
   * @param message the EdgeRpcNotification
   */
  private handleEdgeRpcNotification(edgeRpcNotification: EdgeRpcNotification): void {
    const edgeId = EDGE_RPC_NOTIFICATION.PARAMS.EDGE_ID;
    const message = EDGE_RPC_NOTIFICATION.PARAMS.PAYLOAD;

    const edges = THIS.SERVICE.METADATA.VALUE?.edges ?? {};
    if (edgeId in edges) {
      const edge = edges[edgeId];

      switch (MESSAGE.METHOD) {
        case EDGE_CONFIG_NOTIFICATION.METHOD:
          EDGE.IS_ONLINE = true; // Mark Edge as online
          EDGE.HANDLE_EDGE_CONFIG_NOTIFICATION(message as EdgeConfigNotification);
          break;

        case CURRENT_DATA_NOTIFICATION.METHOD:
          EDGE.HANDLE_CURRENT_DATA_NOTIFICATION(message as CurrentDataNotification);
          break;

        case SYSTEM_LOG_NOTIFICATION.METHOD:
          EDGE.HANDLE_SYSTEM_LOG_NOTIFICATION(message as SystemLogNotification);
          break;
      }
    }
  }
}
