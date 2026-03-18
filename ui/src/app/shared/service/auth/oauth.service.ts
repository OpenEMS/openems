import { inject, Injectable } from "@angular/core";
import { Router } from "@angular/router";
import { App, URLOpenListenerEvent } from "@capacitor/app";
import { CookieService } from "ngx-cookie-service";
import { PlatFormService } from "src/app/platform.service";
import { environment } from "src/environments";
import { JsonrpcRequest } from "../../jsonrpc/base";
import { JsonRpcUtils } from "../../jsonrpc/jsonrpcutils";
import { User } from "../../jsonrpc/shared";
import { States } from "../../ngrx-store/states";
import { Language } from "../../type/language";
import { StringUtils } from "../../utils/string/string.utils";
import { Service } from "../service";
import { UserService } from "../user.service";
import { AuthenticateWithOAuthRequest, AuthenticateWithOAuthResponse } from "./jsonrpc";

@Injectable({ providedIn: "root" })
export class OAuthService {

    public static readonly REFRESH_TOKEN: string = "refresh_token";

    private service: Service = inject(Service);
    private cookieService: CookieService = inject(CookieService);
    private router: Router = inject(Router);
    private userService: UserService = inject(UserService);
    private platformService: PlatFormService = inject(PlatFormService);

    constructor() {
        App.addListener("appUrlOpen", (data: URLOpenListenerEvent) => {
            const isApp = this.platformService.getIsApp();
            if (isApp) {
                this.router.navigate(["/oauthcallback"], { queryParams: { code: data.url.split("code=")[1] ?? null } });
            }
        });
    }

    public static isOAuth(cookieService: CookieService) {
        return cookieService.check(OAuthService.REFRESH_TOKEN) && environment.backend === "OpenEMS Backend";
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

    /**
        * Executes when websocket is 'online'
        *
        * @returns
        */
    public async startOAuth(): Promise<void> {

        const tokenResponse = await this.getTokenByRefreshToken();
        if (tokenResponse == null) {
            return;
        }
        this.completeAuthentication(tokenResponse);
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
    public getTokenByCode(code: string, oauthState: { state: string; identifier: string; } | null) {
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
            this.completeAuthentication(response);
        }).catch(() => this.service.websocket.logout());
    }

    /**
     * Executes after successfull authentication.
     *
     * @param tokenResponse the token response from authentication
    */
    public completeAuthentication(tokenResponse: AuthenticateWithOAuthResponse) {
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
    public async getTokenByRefreshToken(): Promise<AuthenticateWithOAuthResponse | null> {
        const refreshToken = this.getRefreshToken();

        if (refreshToken == null && StringUtils.isValidString(refreshToken)) {
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
            this.service.websocket.logout();
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
        this.cookieService.set(OAuthService.REFRESH_TOKEN, JSON.stringify(result.refreshToken), 14, "/");
    }
}
