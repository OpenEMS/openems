// @ts-strict-ignore
import { Injectable, signal, WritableSignal } from "@angular/core";
import { Router } from "@angular/router";
import { Capacitor } from "@capacitor/core";
import { TranslateService } from "@ngx-translate/core";
import { SavePassword } from "capacitor-ios-autofill-save-password";
import { CookieService } from "ngx-cookie-service";
import { delay, retry } from "rxjs/operators";
import { webSocket, WebSocketSubject } from "rxjs/webSocket";
import { v4 as uuidv4 } from "uuid";
import { InitiateConnect } from "src/app/edge/settings/app/oauth/jsonrpc/initiateConnect";
import { PlatFormService } from "src/app/platform.service";
import { environment } from "src/environments";

import { AuthenticationFailedError, DuplicateAuthenticationFailureException } from "../errors.ts/errors";
import { WebsocketInterface } from "../interface/websocketInterface";
import { JsonrpcMessage, JsonrpcNotification, JsonrpcRequest, JsonrpcResponse, JsonrpcResponseError, JsonrpcResponseSuccess } from "../jsonrpc/base";
import { JsonRpcUtils } from "../jsonrpc/jsonrpcutils";
import { CurrentDataNotification } from "../jsonrpc/notification/currentDataNotification";
import { EdgeConfigNotification } from "../jsonrpc/notification/edgeConfigNotification";
import { EdgeRpcNotification } from "../jsonrpc/notification/edgeRpcNotification";
import { SystemLogNotification } from "../jsonrpc/notification/systemLogNotification";
import { AuthenticateWithPasswordRequest } from "../jsonrpc/request/authenticateWithPasswordRequest";
import { AuthenticateWithTokenRequest } from "../jsonrpc/request/authenticateWithTokenRequest";
import { EdgeRpcRequest } from "../jsonrpc/request/edgeRpcRequest";
import { LogoutRequest } from "../jsonrpc/request/logoutRequest";
import { RegisterUserRequest } from "../jsonrpc/request/registerUserRequest";
import { SubscribeChannelsRequest } from "../jsonrpc/request/subscribeChannelsRequest";
import { AuthenticateResponse } from "../jsonrpc/response/authenticateResponse";
import { User } from "../jsonrpc/shared";
import { States } from "../ngrx-store/states";
import { Language } from "../type/language";
import { TMutable } from "../type/utility";
import { ArrayUtils } from "../utils/array/array.utils";
import { PromiseUtils } from "../utils/promise/promise.utils";
import { AuthService } from "./auth/auth.service";
import { AuthenticateWithOAuth2Response, AuthenticateWithOAuthRequest } from "./auth/jsonrpc";
import { OAuthService } from "./auth/oauth.service";
import { Pagination } from "./pagination";
import { Service } from "./service";
import { UserService } from "./user.service";
import { WsData } from "./wsdata";

@Injectable()
export class Websocket implements WebsocketInterface {

    public static readonly REQUEST_TIMEOUT = 500;
    public pendingRequests: Map<string | number, Promise<JsonrpcResponse>> = new Map();

    public status:
        "initial" // before first connection attempt
        | "connecting" // trying to connect to backend
        | "authenticating" // sent authentication request; waiting for response
        | "waiting for credentials" // login is required. Waiting for credentials input
        | "online" // logged in + normal operation
        | "failed" /* connection failed*/ = "initial";

    public readonly state: WritableSignal<States> = signal(States.WEBSOCKET_NOT_YET_CONNECTED);

    private readonly wsdata = new WsData();

    private socket: WebSocketSubject<any>;
    private previousErrors: Map<string, Error> = new Map();

    constructor(
        private service: Service,
        private translate: TranslateService,
        private cookieService: CookieService,
        private router: Router,
        private userService: UserService,
        private pagination: Pagination,
        private authService: AuthService,
        private platFormService: PlatFormService,
    ) {
        service.websocket = this;

        // try to auto connect using token or session_id
        setTimeout(() => {
            this.connect();
        });
    }

    /**
     * Initiates connection for oauth
     *
     * @returns a {@link AuthenticateWithOAuth2Response}
     */
    public initiateConnect() {
        return new Promise<AuthenticateWithOAuth2Response>((res, rej) => {
            this.state.set(States.NOT_AUTHENTICATED);
            return this.sendRequest<AuthenticateWithOAuth2Response>(new AuthenticateWithOAuthRequest({
                payload: new JsonrpcRequest(InitiateConnect.METHOD, { ...OAuthService.getOem(), ...OAuthService.getRedirectUri(this.platFormService) }),
            })).then(async response => {
                const result = response.result as { identifier: string, loginUrl: string, state: string };
                window.open(result.loginUrl, "_self");
                this.cookieService.set("oauthredirectstate", JSON.stringify({ ...result, href: "oauthcallback" }), 1, "/");
                res(response);
            }).catch((error) => {
                rej(error);
            });
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
                this.state.set(States.AUTHENTICATED);
                const authenticateResponse = (r as AuthenticateResponse).result;

                if (request instanceof AuthenticateWithPasswordRequest) {
                    if (Capacitor.getPlatform() === "ios") {
                        SavePassword.promptDialog({
                            username: request.params.username,
                            password: request.params.password,
                        });
                    }
                }

                const userLangKey = Language.getByKey(authenticateResponse.user.language?.toLowerCase());
                const demoLangKey = Language.getByKey(localStorage.DEMO_LANGUAGE);

                const language = demoLangKey ?? userLangKey ?? Language.SYSTEM ?? Language.DEFAULT;
                localStorage.LANGUAGE = language.key;
                this.service.setLang(language);
                this.status = "online";

                // received login token -> save in cookie
                this.cookieService.set(AuthService.TOKEN, authenticateResponse.token, { expires: 365, path: "/", sameSite: "Strict", secure: location.protocol === "https:" });
                this.userService.currentUser.set(User.from(authenticateResponse.user));
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

                const initialUrl = this.router.lastSuccessfulNavigation?.initialUrl;
                if (initialUrl == null) {
                    this.router.navigate(["/overview"]);
                    resolve();
                    return;
                }

                const isAuthenticatedNavi = initialUrl.toString().split("/").length > 2;
                if (isAuthenticatedNavi) {
                    this.router.navigate([initialUrl.toString().split("?")[0]], { queryParams: initialUrl.queryParams });
                    resolve();
                    return;
                }

                this.router.navigate(["/overview"]);
                resolve();
            }).catch(reason => {
                this.checkErrorCode(reason);
                resolve();
            });
        });
    }

    /**
     * Logs out by sending a logout JSON-RPC Request.
     */
    public logout() {
        this.sendRequest(new LogoutRequest()).then(response => {
            this.onLoggedOut();
        }).catch(reason => {
            console.error(reason);
            this.router.navigate(["/login"]);
        });
    }

    /**
     * Sends a JSON-RPC Request to a Websocket and promises a callback.
     *
     * @param request the JSON-RPC Request
     */
    public sendRequest<T extends JsonrpcResponseSuccess = JsonrpcResponseSuccess>(request: JsonrpcRequest): Promise<T> {
        if (
            // logged in + normal operation
            this.status == "online"
            // otherwise only authentication request allowed
            || (request instanceof AuthenticateWithOAuthRequest || request instanceof AuthenticateWithPasswordRequest || request instanceof AuthenticateWithTokenRequest || request instanceof RegisterUserRequest)) {

            return new Promise((resolve, reject) => {
                this.wsdata.sendRequest<T>(this.socket, request)
                    .then(response => resolve(response))
                    .catch(async reason => await this.handleJsonRpcError(reason, request, reject, resolve));
            });

        } else {
            return Promise.reject("Websocket is not connected or authenticated! Unable to send Request: " + JSON.stringify(request));
        }
    }

    public onLoggedOut(): void {
        this.status = "waiting for credentials";
        this.cookieService.delete("token", "/");
        this.authService.logout();
        this.service.onLogout();
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
                if (this.status == "online") {
                    clearInterval(interval);
                    this.sendRequest(request)
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
        if (this.status != "online") {
            console.warn("Websocket is not connected! Unable to send Notification", notification);
        }
        this.wsdata.sendNotification(this.socket, notification);
    }

    public initiateWebsocket() {
        this.socket = webSocket({
            url: environment.url,
            openObserver: {
                next: (value) => {
                    this.state.set(States.WEBSOCKET_NOT_YET_CONNECTED);
                    // Websocket connection is open
                    if (environment.debugMode) {
                        console.info("Websocket connection opened");
                    }

                    const token = this.cookieService.get("token");
                    const oAuthRedirectState = this.cookieService.get("oauthredirectstate");
                    const refreshToken = this.cookieService.get("refresh_token");
                    if (token) {
                        this.state.set(States.AUTHENTICATING_WITH_TOKEN);

                        // Login with Session Token
                        this.login(new AuthenticateWithTokenRequest({ token: token }));
                        this.status = "authenticating";
                    }
                    else {
                        // No Token -> directly ask for Login credentials
                        this.state.set(States.NOT_AUTHENTICATED);
                        this.status = "waiting for credentials";

                        // Needed for oauth authentication
                        if (refreshToken == "" && oAuthRedirectState == "") {
                            this.router.navigate(["login"]);
                        }
                    }
                },
            },
            closeObserver: {
                next: (value) => {
                    // Websocket connection is closed. Auto-Reconnect starts.
                    this.state.set(States.WEBSOCKET_CONNECTION_CLOSED);
                    if (environment.debugMode) {
                        console.info("Websocket connection closed");
                    }
                    // trying to connect
                    this.state.set(States.WEBSOCKET_CONNECTING);
                    this.status = "connecting";
                },
            },
        });
    }

    private async handleJsonRpcError(reason: JsonrpcResponseError, request: JsonrpcRequest, reject: PromiseUtils.Types.Reject, resolve: PromiseUtils.Types.Resolve): Promise<void> {
        if (environment.debugMode) {
            if (reason instanceof JsonrpcResponseError) {
                console.warn("Request failed [" + request.method + "]", reason.error);
                if (request instanceof EdgeRpcRequest && reason.error?.code == 3000 /* Edge is not connected */) {
                    const edges = this.service.metadata.value?.edges ?? {};
                    if (request.params.edgeId in edges) {
                        edges[request.params.edgeId].isOnline = false;
                    }
                }
            } else {
                console.warn("Request failed [" + request.method + "]", reason);
            }
        }
        const errorCode = reason?.error?.code ?? null;
        switch (errorCode) {
            case AuthenticationFailedError.id: {
                this.state.set(States.AUTHENTICATION_FAILED);

                if (request instanceof AuthenticateWithOAuthRequest && (request.params["payload"].method === "getTokenByRefreshToken")) {
                    this.onLoggedOut();
                    reject(reason);
                    return;
                }

                await this.authService.handleAuthenticationFailed();
                const newRequest: TMutable<JsonrpcRequest> = { ...request, id: uuidv4() };
                const [err, response] = await JsonRpcUtils.handleResponse(this.wsdata.sendRequest(this.socket, newRequest));
                if (err) {
                    if (ArrayUtils.containsStrings(Array.from(this.previousErrors.values()).map(el => el.name), [err.name])) {
                        this.logout();
                        reject(new DuplicateAuthenticationFailureException());
                        return;
                    }

                    if (err != null) {
                        this.previousErrors.set(request.id, err);
                    }
                    reject(err);
                    return;
                }

                // Navigate to same page, avoiding hard refresh
                this.router.navigate([this.router.url], { skipLocationChange: false, onSameUrlNavigation: "reload" as any, replaceUrl: true });
                this.state.set(States.AUTHENTICATED);
                resolve(response);
                break;
            }
            default:
                reject(reason);
                break;
        }
    }

    /**
   * Opens a connection using a stored token. Called once by constructor
   */
    private connect() {
        this.state.set(States.WEBSOCKET_NOT_YET_CONNECTED);
        if (this.status != "initial") {
            return;
        }
        // trying to connect
        this.state.set(States.WEBSOCKET_CONNECTING);
        this.status = "connecting";

        if (environment.debugMode) {
            console.info("Websocket connecting to URL [" + environment.url + "]");
        }

        /*
        * Open Websocket connection + define onOpen/onClose callbacks.
        */
        this.initiateWebsocket();

        this.socket.pipe(
            // Websocket Auto-Reconnect
            retry({
                delay(error, retryCount) {
                    return error.pipe(delay(1000));
                },
            }),

        ).subscribe(originalMessage => {
            // Receive message from server
            const message: JsonrpcRequest | JsonrpcNotification | JsonrpcResponseSuccess | JsonrpcResponseError =
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
                    if (message.method == EdgeRpcNotification.METHOD && "payload" in message.params) {
                        const m = message as EdgeRpcNotification;
                        const payload = m.params.payload;
                        console.info("Notification [" + m.params.edgeId + "] [" + payload["method"] + "]", payload["params"]);
                    } else {
                        console.info("Notification [" + message.method + "]", message.params);
                    }
                }
                this.onNotification(message);
            }

        }, error => {
            this.onError(error);

        }, () => {
            this.status = "failed";
            this.onClose();
        });
    }

    private checkErrorCode(reason: JsonrpcResponseError) {

        // TODO create global Errorhandler for any type of error
        switch (reason?.error?.code) {
            case 1003:
                this.service.toast(this.translate.instant("LOGIN.AUTHENTICATION_FAILED"), "danger");
                this.onLoggedOut();
                break;
            case 1:
                this.service.toast(this.translate.instant("LOGIN.REQUEST_TIMEOUT"), "danger");
                this.status = "waiting for credentials";
                this.service.onLogout();
                break;
            default:
                this.onLoggedOut();
        }
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
        const edgeId = edgeRpcNotification.params.edgeId;
        const message = edgeRpcNotification.params.payload;

        const edge = this.service.currentEdge();

        if (edge == null) {
            const unsubscribeFromChannelsRequest = new EdgeRpcRequest({ edgeId: edgeId, payload: new SubscribeChannelsRequest([]) });
            this.sendRequest(unsubscribeFromChannelsRequest);
            return;
        }

        if (edge.id !== edgeId) {
            console.error("EdgeId doesnt match");
            return;
        }

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
