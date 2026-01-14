import { computed, Directive, effect, signal, WritableSignal } from "@angular/core";
import { Router } from "@angular/router";
import { CookieService } from "ngx-cookie-service";
import { DeviceDetectorService } from "ngx-device-detector";
import { PlatFormService } from "src/app/platform.service";
import { States } from "../../ngrx-store/states";
import { RouteService } from "../route.service";
import { Service } from "../service";
import { UserService } from "../user.service";
import { OAuthService } from "./oauth.service";

@Directive()
export class AuthService {

    public static readonly TOKEN: string = "token";

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
        private oAuthService: OAuthService,
    ) {

        const context = effect(() => {
            const websocketStatus = this.service.websocket.state();

            if (States.isAtLeast(websocketStatus, States.WEBSOCKET_CONNECTED) && OAuthService.isOAuth(this.cookieService)) {
                oAuthService.startOAuth();
                context.destroy();
            }

            if (websocketStatus === States.NOT_AUTHENTICATED && this.cookieService.check(AuthService.TOKEN) && this.cookieService.check(OAuthService.REFRESH_TOKEN)) {
                this.service.websocket.logout();
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
