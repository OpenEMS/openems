import { Component, effect } from "@angular/core";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { ActivatedRoute, Router, RouterModule } from "@angular/router";
import { FormlyModule } from "@ngx-formly/core";
import { CookieService } from "ngx-cookie-service";
import { JsonrpcResponseError } from "src/app/shared/jsonrpc/base";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { PipeComponentsModule } from "src/app/shared/pipe/pipe.module";
import { Edge, Service, Websocket } from "src/app/shared/shared";
import { environment } from "src/environments";
import { CommonUiModule } from "../../../../shared/common-ui.module";
import { Connect } from "./jsonrpc/connect";
import { DisconnectOAuthConnection } from "./jsonrpc/disconnectOAuthConnection";
import { GetAllOAuthProvider, OAuthMetaInfo } from "./jsonrpc/getAllOAuthProvider";
import { ConnectionState, GetOAuthConnectionState } from "./jsonrpc/getOAuthConnectionState";
import { InitiateConnect } from "./jsonrpc/initiateConnect";

@Component({
    selector: OAuthIndexComponent.SELECTOR,
    templateUrl: "./oauth.component.html",
    standalone: true,
    imports: [
        CommonUiModule,
        PipeComponentsModule,
        RouterModule,
        FormsModule,
        FormlyModule,
        ReactiveFormsModule,
    ],
})
export class OAuthIndexComponent {

    public static readonly OAUTH_CORE_COMPONENT_ID = "_oauth2";
    private static readonly SELECTOR = "oauth-index";

    protected metaInfos: (OAuthMetaInfo & { connectionState: null | ConnectionState })[] = [];

    private edge: undefined | Edge;

    public constructor(
        private service: Service,
        private websocket: Websocket,
        private cookieService: CookieService,
        private route: ActivatedRoute,
        private router: Router,
    ) {
        effect(async () => {
            this.edge = await this.service.getCurrentEdge();

            let response: GetAllOAuthProvider.Response;
            try {
                response = await this.edge.sendRequest<GetAllOAuthProvider.Response>(this.websocket, new ComponentJsonApiRequest({
                    componentId: OAuthIndexComponent.OAUTH_CORE_COMPONENT_ID,
                    payload: new GetAllOAuthProvider.Request(),
                }));
            } catch (e) {
                if (e instanceof JsonrpcResponseError) {
                    this.service.toast("Failed to get OAuth provider: " + e.error.message, "danger");
                } else {
                    this.service.toast("Failed to get OAuth provider: " + (e ? e.toString() : ""), "danger");
                }
                return;
            }

            this.metaInfos = response.result.metaInfos.sort((a, b) => a.identifier.localeCompare(b.identifier))
                .map(e => {
                    return { ...e, connectionState: null };
                });

            this.metaInfos.forEach(metaInfo => {
                // TODO theoretically could be one too much if connecting a account
                this.getConnectionState(metaInfo.identifier).then(state => {
                    if (!metaInfo.connectionState) {
                        metaInfo.connectionState = state;
                    }
                });
            });

            const state = this.route.snapshot.queryParams["state"];

            if (!state) {
                return;
            }
            debugLog("State", state);
            const code = this.route.snapshot.queryParams["code"];
            const oauthRedirectState = JSON.parse(this.cookieService.get("oauthredirectstate")) as { href: string, state: string, oauthprovider: string };

            // remove query params in url
            this.router.navigate(["device/" + (this.edge.id) + "/settings/app/oauth"]);

            if (state !== oauthRedirectState.state) {
                this.service.toast("states do not match. try again", "warning");
                return;
            }

            if (!code) {
                return;
            }
            debugLog("Code", code);
            const metaInfo = this.metaInfos.find(e => e.identifier === oauthRedirectState.oauthprovider);
            if (!metaInfo) {
                this.service.toast("Unable to find oauth provider with name '" + oauthRedirectState.oauthprovider + "'", "warning");
                return;
            }

            this.edge.sendRequest(this.websocket, new ComponentJsonApiRequest({
                componentId: OAuthIndexComponent.OAUTH_CORE_COMPONENT_ID,
                payload: new Connect.Request({ identifier: metaInfo.identifier, code: code, state: oauthRedirectState.state }),
            })).then(_ => {
                metaInfo.connectionState = "CONNECTED";
            }).catch(error => {
                this.service.toast("Unable to connect: " + error.error?.message, "danger");
            });
        });
    }

    protected async initiateConnect(identifier: string) {
        const edge = this.edge;
        if (!edge) {
            return;
        }

        try {
            // TODO add theme theme.theme
            const response = await edge.sendRequest<InitiateConnect.Response>(this.websocket, new ComponentJsonApiRequest({
                componentId: OAuthIndexComponent.OAUTH_CORE_COMPONENT_ID,
                payload: new InitiateConnect.Request({ identifier: identifier }),
            }));

            const result = response.result;
            // TODO add check if current base url is redirect url

            this.cookieService.set("oauthredirectstate", JSON.stringify({ state: result.state, href: window.location.pathname, oauthprovider: identifier }), { expires: 1, path: "/" });

            let fullUrl = result.url
        + "?client_id=" + result.clientId
        + "&response_type=code"
        + "&redirect_uri=" + result.redirectUri
        + "&scope=" + result.scopes.join(" ")
        + "&state=" + result.state;

            if (result.codeChallenge && result.codeChallengeMethod) {
                fullUrl = fullUrl + "&code_challenge=" + result.codeChallenge
          + "&code_challenge_method=" + result.codeChallengeMethod;
            }

            debugLog("Redirect", fullUrl);

            window.open(fullUrl, "_self");
        } catch (e) {
            if (e instanceof JsonrpcResponseError) {
                this.service.toast("Unable to initiate connect: " + e.error.message, "danger");
            } else {
                this.service.toast("Unable to initiate connect: " + e, "danger");
            }
        }
    }

    protected async disconnectOAuthConnection(identifier: string) {
        await this.edge?.sendRequest(this.websocket, new ComponentJsonApiRequest({
            componentId: OAuthIndexComponent.OAUTH_CORE_COMPONENT_ID,
            payload: new DisconnectOAuthConnection.Request({ identifier: identifier }),
        }));

        const metaInfo = this.metaInfos.find(e => e.identifier === identifier);
        if (metaInfo) {
            metaInfo.connectionState = "NOT_CONNECTED";
        }
    }

    private async getConnectionState(identifier: string): Promise<ConnectionState> {
        const edge = this.edge;
        if (!edge) {
            throw Error();
        }

        const response = await edge.sendRequest<GetOAuthConnectionState.Response>(this.websocket, new ComponentJsonApiRequest({
            componentId: OAuthIndexComponent.OAUTH_CORE_COMPONENT_ID,
            payload: new GetOAuthConnectionState.Request({ identifier: identifier }),
        }));

        return response.result.connectionState;
    }

}

function debugLog(message: string, ...optionalParams: any[]) {
    if (environment.debugMode) {
        console.log(message, optionalParams);
    }
}
