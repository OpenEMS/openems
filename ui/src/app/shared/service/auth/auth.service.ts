import { computed, Directive, effect, inject, runInInjectionContext, signal, untracked, WritableSignal } from "@angular/core";
import { Router } from "@angular/router";
import { CookieService } from "ngx-cookie-service";
import { environment } from "src/environments";
import { States } from "../../ngrx-store/states";
import { Service } from "../service";
import { Websocket } from "../websocket";
import { AUTHENTICATION_STATE, OAuthService } from "./oauth.service";

@Directive()
export class AuthService {

    public static readonly TOKEN: string = "token";

    public redirectURI$ = computed(() => this.redirectURI());
    private authenticationAlreadyCompleted: boolean = false;
    private redirectURI: WritableSignal<string | null> = signal(null);
    private cookieService = inject(CookieService);
    private oAuthService = inject(OAuthService);
    private service = inject(Service);
    private router = inject(Router);

    constructor() {

        const context = effect(() => {
            const websocketStatus = this.service.websocket.state();
            const isOAuth = OAuthService.isOAuth(this.cookieService);
            if (States.isAtLeast(websocketStatus, States.WEBSOCKET_CONNECTED) && isOAuth) {
                this.oAuthService.startOAuth();
                context.destroy();
            }

            if (!isOAuth && environment.backend === "OpenEMS Edge" && this.cookieService.check(AuthService.TOKEN) === false) {
                this.router.navigate(["/login"]);
            }

            if (websocketStatus === States.NOT_AUTHENTICATED && this.cookieService.check(AuthService.TOKEN) && this.cookieService.check(OAuthService.REFRESH_TOKEN)) {
                this.service.websocket.logout();
            }
        });
    }

    public async authenticate(websocket: Websocket) {
        return new Promise<void>((resolve) => {

            if (States.isAtLeast(websocket.state(), States.AUTHENTICATED)) {
                resolve();
                return;
            }

            const dispose = runInInjectionContext(websocket.injector, () => {
                return untracked(() => {
                    return effect(async () => {
                        const state = websocket.state();

                        if (States.isAtLeast(state, States.WEBSOCKET_CONNECTED) && this.oAuthService.getCurrentState() != AUTHENTICATION_STATE.AUTHENTICATING && !States.isAtLeast(state, States.AUTHENTICATED) && OAuthService.isOAuth(this.cookieService)) {
                            await this.oAuthService.startOAuth();
                            resolve();
                            dispose.destroy();
                        }

                        if (state === States.NOT_AUTHENTICATED && this.cookieService.check(AuthService.TOKEN) && this.cookieService.check(OAuthService.REFRESH_TOKEN)) {
                            this.service.websocket.logout();
                            resolve();
                        }

                        if (States.isAtLeast(state, States.AUTHENTICATED)) {
                            resolve();
                            dispose.destroy();
                        }
                    });
                });
            });
        });
    }

    /**
     * Handles authentication failed
     */
    public async handleAuthenticationFailed() {
        if (this.authenticationAlreadyCompleted == false) {
            if (OAuthService.isOAuth(this.cookieService)) {
                this.oAuthService.getTokenByRefreshToken();
                return;
            }
            this.getToken();
            return;
        }

        this.authenticationAlreadyCompleted = false;

        await this.oAuthService.startOAuth();
        this.authenticationAlreadyCompleted = true;
    }

    /** Acts on logout */
    public logout() {
        this.cookieService.delete(AuthService.TOKEN, "/");
        this.cookieService.delete(OAuthService.REFRESH_TOKEN);
        this.cookieService.delete("oauthredirectstate");
    }

    private getToken() {
        return this.cookieService.get(AuthService.TOKEN);
    }
}
