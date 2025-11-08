import { Component, effect, inject, Input } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { CookieService } from "ngx-cookie-service";
import { filter, Subscription } from "rxjs";
import { v4 as uuidv4 } from "uuid";
import { PlatFormService } from "src/app/platform.service";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { FlatWidgetButtonComponent } from "src/app/shared/components/flat/flat-widget-button/flat-widget-button";
import { HelpPopoverButtonComponent } from "src/app/shared/components/shared/view-component/help-popover/help-popover";
import { JsonrpcResponseError } from "src/app/shared/jsonrpc/base";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { ChannelAddress, Edge, Service, Websocket } from "src/app/shared/shared";
import { Environment, environment } from "src/environments";
import { GetAppAssistant } from "../../jsonrpc/getAppAssistant";
import { Connect } from "../../oauth/jsonrpc/connect";
import { DisconnectOAuthConnection } from "../../oauth/jsonrpc/disconnectOAuthConnection";
import { ConnectionState } from "../../oauth/jsonrpc/getOAuthConnectionState";
import { InitiateConnect } from "../../oauth/jsonrpc/initiateConnect";
import { OAuthIndexComponent } from "../../oauth/oauth.component";
import { mapChannelValueToConnectionState } from "./configuration-oauth-utils";


@Component({
    selector: ConfigurationOAuthComponent.SELECTOR,
    templateUrl: "./configuration-oauth.component.html",
    standalone: true,
    imports: [
        CommonUiModule,
        FlatWidgetButtonComponent,
        HelpPopoverButtonComponent,
    ],
})
export class ConfigurationOAuthComponent {
    private static readonly SELECTOR = "configuration-oauth";

    @Input() public instanceProperties: { [key: string]: string | number } = {};
    @Input({ required: true }) public step!: Extract<GetAppAssistant.AppConfigurationStep, { type: GetAppAssistant.AppConfigurationStepType.OAUTH }>;

    protected environment: Environment = environment;
    protected connectionState: ConnectionState = "VALIDATING";
    protected isApp: boolean = false;

    private readonly channelSubscriptionId = uuidv4();

    private service = inject(Service);
    private websocket = inject(Websocket);
    private cookieService = inject(CookieService);
    private route = inject(ActivatedRoute);
    private router = inject(Router);
    private platformService = inject(PlatFormService);
    private translateService = inject(TranslateService);

    private edge: Edge | null = null;

    constructor() {
        effect(async onCleanup => {
            this.isApp = this.platformService.getIsApp();

            const currentEdge = this.service.currentEdge();

            this.edge = currentEdge;
            if (!currentEdge) {
                return;
            }

            const oauthProviderName = this.getOAuthProvider();
            if (!oauthProviderName) {
                return;
            }

            const channel = new ChannelAddress(oauthProviderName, "OauthConnectionState");
            currentEdge.subscribeChannels(this.websocket, this.channelSubscriptionId, [channel]);

            const subscription: Subscription = currentEdge.currentData.pipe(
                filter(currentData => currentData !== null),
            ).subscribe((currentData) => {
                const channelValue: number = currentData.channel[channel.toString()];

                this.connectionState = mapChannelValueToConnectionState(channelValue);;
            });

            onCleanup(() => {
                subscription.unsubscribe();
                currentEdge.unsubscribeChannels(this.websocket, this.channelSubscriptionId);
            });

            this.connectCode();
        });
    }

    protected async initiateConnect() {
        const identifier = this.getOAuthProvider();
        const edge = this.edge;
        if (!edge || !identifier || this.connectionState === "CONNECTED") {
            return;
        }

        try {
            const response = await edge.sendRequest<InitiateConnect.Response>(this.websocket, new ComponentJsonApiRequest({
                componentId: OAuthIndexComponent.OAUTH_CORE_COMPONENT_ID,
                payload: new InitiateConnect.Request({ identifier: identifier }),
            }));

            const result = response.result;

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

            window.open(fullUrl, "_self");
        } catch (e) {
            if (e instanceof JsonrpcResponseError) {
                this.service.toast("Unable to initiate connect: " + e.error.message, "danger");
            } else {
                this.service.toast("Unable to initiate connect: " + e, "danger");
            }
        }
    }

    protected async disconnectOAuthConnection() {
        const identifier = this.getOAuthProvider();
        if (!identifier || this.connectionState !== "CONNECTED") {
            return;
        }
        await this.edge?.sendRequest(this.websocket, new ComponentJsonApiRequest({
            componentId: OAuthIndexComponent.OAUTH_CORE_COMPONENT_ID,
            payload: new DisconnectOAuthConnection.Request({ identifier: identifier }),
        }));
    }

    protected getOAuthProvider(): string | null {
        const result = this.instanceProperties?.[this.step.params.componentIdPropertyPath as string];
        if (result !== null && result !== undefined && typeof result === "string") {
            return result;
        }
        return null;
    }

    private async connectCode() {
        const state = this.route.snapshot.queryParams["state"];

        if (!state) {
            return;
        }
        const code = this.route.snapshot.queryParams["code"];
        const oauthRedirectState = JSON.parse(this.cookieService.get("oauthredirectstate")) as { href: string, state: string, oauthprovider: string };

        if (oauthRedirectState.oauthprovider !== this.getOAuthProvider()) {
            return;
        }

        // remove query params from url
        this.router.navigate([this.router.url.split("?")[0]]);

        if (state !== oauthRedirectState.state) {
            this.service.toast(this.translateService.instant("Edge.Config.App.OAUTH.STATES_MISMATCH"), "warning");
            return;
        }

        if (code === undefined || code === null) {
            return;
        }
        const identifier = this.getOAuthProvider();
        if (identifier === null) {
            return;
        }

        try {
            await this.edge?.sendRequest(this.websocket, new ComponentJsonApiRequest({
                componentId: OAuthIndexComponent.OAUTH_CORE_COMPONENT_ID,
                payload: new Connect.Request({ identifier: identifier, code: code, state: oauthRedirectState.state }),
            }));
        } catch (error: JsonrpcResponseError | any) {
            this.service.toast(this.translateService.instant("Edge.Config.App.OAUTH.UNABLE_TO_CONNECT_CODE", { error: error.error?.message }), "danger");
        }
    }

}
