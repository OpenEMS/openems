// @ts-strict-ignore
import { Component, effect } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { States } from "../shared/ngrx-store/states";
import { OAuthService } from "../shared/service/oauth/oauth.service";
import { Service, Websocket } from "../shared/shared";

@Component({
    selector: "oauth-callback",
    template: "",
    standalone: false,
})
export class OAuthCallBackComponent {

    constructor(
        private route: ActivatedRoute,
        private oAuthService: OAuthService,
        private websocket: Websocket,
        private service: Service,
    ) {

        const context = effect(() => {
            const status = this.websocket.state();
            if (States.isAtLeast(status, States.WEBSOCKET_CONNECTED)) {
                OAuthCallBackComponent.processQueryParams(this.route, this.oAuthService);
                context.destroy();
            };
        });

        // const interval = setInterval(() => {
        //     if (this.websocket.status == "waiting for credentials") {
        //         // this.service.toast("Here", "success")
        //         OAuthCallBackComponent.some(this.route, this.oAuthService);
        //         clearInterval(interval);
        //     }
        // }, 100)

        // effect(() => {
        //     if (this.websocket.()) {
        // //         this.ngOnInit();
        //     }
        // })
    }


    // public async ngOnInit() {
    //     OAuthCallBackComponent.some(this.route, this.oAuthService);
    // }

    public static async processQueryParams(route: ActivatedRoute, oAuthService: OAuthService) {
        const code = route.snapshot.queryParams["code"];

        const oauthState = oAuthService.getOAuthState();
        if (oauthState == null) {
            return;
        }


        await oAuthService.getTokenByCode(code, oauthState);
    }
}


// effect(() => {
//     const code = this.route.snapshot.queryParams["code"];
//     const refresh_token = this.oAuthService.getRefreshToken();
//     const oauthState = this.oAuthService.getOAuthState(this.cookies);

//     if (this.isWebsocketOnline()) {
//         websocket.state.set(States.AUTHENTICATING);
//         this.oAuthService.onRedirect(code, oauthState);
//     }
// })

// const interval = setInterval(async () => {
//     const code = this.route.snapshot.queryParams["code"];
//     const oauthState = this.oAuthService.getOAuthState(this.cookies);
//     if (!oauthState) {
//         return;
//     }

//     if (this.isWebsocketOnline()) {
//         websocket.state.set(States.AUTHENTICATING);

//         await this.oAuthService.onRedirect(code, oauthState);
//         clearInterval(interval);
//     }
// }, 100);

// effect(async () => {
//     const isWebsocketOnline = this.oAuthService.service.websocket.state() == States.WEBSOCKET_CONNECTED;
//     if (isWebsocketOnline) {
//         await this.oAuthService.onRedirect(code, oauthState);
//     }
// })
