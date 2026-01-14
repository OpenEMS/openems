import { computed, Directive, effect, signal, WritableSignal } from "@angular/core";
import { Router } from "@angular/router";
import { App, URLOpenListenerEvent } from "@capacitor/app";
import { CookieService } from "ngx-cookie-service";
import { DeviceDetectorService } from "ngx-device-detector";
import { PlatFormService } from "src/app/platform.service";
import { environment } from "src/environments";
import { JsonrpcRequest } from "../../jsonrpc/base";
import { JsonRpcUtils } from "../../jsonrpc/jsonrpcutils";
import { User } from "../../jsonrpc/shared";
import { States } from "../../ngrx-store/states";
import { Language } from "../../type/language";
import { PromiseUtils } from "../../utils/promise/promise.utils";
import { RouteService } from "../route.service";
import { Service } from "../service";
import { UserService } from "../user.service";
import { AuthenticateWithOAuthRequest, AuthenticateWithOAuthResponse } from "./jsonrpc";

@Directive()
export class OAuthService {

    public redirectURI$ = computed(() => this.redirectURI());
    private authenticationAlreadyCompleted: boolean = false;
    private redirectURI: WritableSignal<string | null> = signal(null);

    constructor(
        public service: Service,
        private cookieService: CookieService,
        private userService: UserService,
        private router: Router,
        private deviceService: DeviceDetectorService,
        private platformService: PlatFormService,
        private routeService: RouteService,
    ) {

        const context = effect(() => {
            const websocketStatus = this.service.websocket.state();

            if (States.isAtLeast(websocketStatus, States.WEBSOCKET_CONNECTED) && this.cookieService.check("refresh_token")) {
                this.onWebsocketOnline();
                context.destroy();
            }
        });

        effect(() => {
            const isApp = platformService.getIsApp();
            if (isApp) {
                this.router.navigate(["/oauthcallback"], { queryParams: { code: this.redirectURI() } });
            }
        });


        if (!this.deviceService.isMobile()) {
            return;
        }
        // App.addListener("pause", () => {
        // effect(() => {
        //     const redirectUri = this.redirectURI();
        //     if (redirectUri == null) {
        //         return;
        //     }
        //     debugger;
        //     this.router.navigate(["/oauthcallback"], { queryParams: { code: redirectUri } });
        // });
        //     this.service.toast("pause", "success", 5000)
        //     // OAuthCallBackComponent.some(this.route, this);
        // })

        // App.addListener("resume", () => {
        //     this.service.websocket.initiateWebsocket();
        //     this.service.toast("resume", "success", 5000)
        //     this.route.queryParams
        //     this.route.queryParams.pipe(take(1)).subscribe(el => {
        //         OAuthCallBackComponent.some(this.route, this);
        //     })
        //     // this.onWebsocketOnline();
        // });

        App.addListener("appUrlOpen", (data: URLOpenListenerEvent) => {
            this.redirectURI.set(data.url.split("code=")[1] ?? null);
        });
    }


    public static getRedirectUri(plaformService: PlatFormService) {
        if (plaformService.getIsApp()) {
            return {};
        }

        return { redirectUri: window.document.baseURI + "oauthcallback" };
    }

    /**
     * Gets the OEM to use.
    *
    * @returns the oem
    */
    public static getOem() {
        return { oem: environment.theme.toLowerCase() };
    }

    /** Acts on logout */
    public logout() {
        this.cookieService.delete("token", "/");
        this.cookieService.delete("refresh_token");
        this.cookieService.delete("oauthredirectstate");
    }

    /**
     * Executes after authentication failed jsonRPC response
     */
    public async onAuthenticationFailed() {
        if (this.authenticationAlreadyCompleted == false) {
            this.getTokenByRefreshToken(this.getRefreshToken());
            return;
        }

        this.authenticationAlreadyCompleted = false;
        await this.onWebsocketOnline();
        this.authenticationAlreadyCompleted = true;
    }

    public async handleWebSocketConnected() {

        const oauthState = this.getOAuthState();
        if (oauthState == null) {
            // window.open("login", "_self")
            this.service.websocket.onLoggedOut();
            return;
        }

        const refresh_token = this.getRefreshToken();
        if (oauthState == null || refresh_token == null) {
            const [err, response] = await PromiseUtils.Functions.handle(this.service.websocket.initiateConnect());

            if (err || response == null) {
                return;
            }
            this.redirectURI.set(response.result.loginUrl);
        }

        this.onWebsocketOnline();
    }

    /**
    * Executes when websocket is 'online'
    *
    * @returns
    */
    public async onWebsocketOnline(): Promise<void> {

        const refreshToken = this.getRefreshToken();
        const tokenResponse = await this.getTokenByRefreshToken(refreshToken);
        if (tokenResponse == null) {
            return;
        }
        this.processTokens(tokenResponse);
    }

    /**
     * Gets the current oauth state
     *
     * @param cookieService the cookie service
     * @returns state and identifiere if available, else null
     */
    public getOAuthState(): { state: string, identifier: string } | null {
        return this.cookieService.check("oauthredirectstate")
            ? JSON.parse(this.cookieService.get("oauthredirectstate")) as { state: string, identifier: string }
            : null;
    }

    /**
     * Executes after redirecting from external authentication service back to UI
     *
     * @param code the current code
     * @param oauthState the current oauth state
     * @returns
     */
    public getTokenByCode(code: string, oauthState: {
        state: string;
        identifier: string;
    }) {
        if (oauthState == null) {
            return;
        }
        this.service.websocket.sendRequest<AuthenticateWithOAuthResponse>(new AuthenticateWithOAuthRequest({
            payload: new JsonrpcRequest("getTokenByCode", {
                identifier: oauthState.identifier,
                code: code,
                ...OAuthService.getOem(),
            }),
        })).then((response: AuthenticateWithOAuthResponse) => {
            const token = response.result;

            this.setTokens(token);
            this.processTokens(response);
        }).catch(() => this.service.websocket.logout());
    }

    /**
     * Executes after successfull authentication
     *
     * @param tokenResponse the token response from authentication
     */
    public processTokens(tokenResponse: AuthenticateWithOAuthResponse) {
        const user = User.from(tokenResponse.result.user);
        if (user == null) {
            this.service.websocket.logout();
            return;
        }

        this.userService.currentUser.set(user);

        this.service.websocket.state.set(States.AUTHENTICATED);
        this.service.websocket.status = "online";

        this.service.metadata.next({
            user: user,
            edges: {},
        });

        const language = Language.getByKey(localStorage.DEMO_LANGUAGE ?? this?.userService?.currentUser()?.language?.toLocaleLowerCase()) ?? Language.DEFAULT;
        localStorage.LANGUAGE = language.key;
        this.service.setLang(language);
        this.service.websocket.status = "online";

        const initialUrl = this.router.lastSuccessfulNavigation?.initialUrl;
        if (initialUrl == null) {
            this.router.navigate(["/overview"]);
            return;
        }

        const isAuthenticatedNavi = initialUrl?.toString()?.split("/")?.length > 2;
        if (isAuthenticatedNavi && initialUrl != null) {
            this.router.navigate([initialUrl.toString().split("?")[0]], { queryParams: initialUrl.queryParams });
            return;
        }

        this.router.navigate(["/overview"]);
    }

    /**
     * Gets the refresh token.
     *
     * @returns the refresh token if existing, else null
     */
    public getRefreshToken(): string | null {

        if (this.cookieService.check("refresh_token") == false) {
            return null;
        }

        return JSON.parse(this.cookieService.get("refresh_token"));
    }

    /**
  * Executes when token and refresh_token is set
  *
  * @param refreshToken the refresh token
  * @returns a authentication response if valid session, else null
  */
    public async getTokenByRefreshToken(refreshToken: string | null): Promise<AuthenticateWithOAuthResponse | null> {

        if (refreshToken == null) {
            this.logout();
            this.service.websocket.logout();
            return null;
        }

        const [err, response] = await JsonRpcUtils.handleResponse<AuthenticateWithOAuthResponse>(this.service.websocket.sendRequest(new AuthenticateWithOAuthRequest({
            payload: new JsonrpcRequest("getTokenByRefreshToken", {
                refreshToken: refreshToken,
                ...OAuthService.getOem(),
            }),
        })));

        if (err || response == null) {
            this.service.websocket.onLoggedOut();
            throw err;
        }

        this.setTokens(response.result);
        return response;
    }

    /**
     * Sets the tokens in the cookie service.
     *
     * @param result the authentication response result
     */
    private setTokens(result: AuthenticateWithOAuthResponse["result"]) {
        this.cookieService.set("refresh_token", JSON.stringify(result.refreshToken), 14, "/");
    }
}
