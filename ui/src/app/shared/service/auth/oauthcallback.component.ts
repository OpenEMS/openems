// @ts-strict-ignore
import { Component, effect, OnDestroy } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { ViewWillLeave } from "@ionic/angular";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { NgxSpinnerModule } from "ngx-spinner";
import { v4 as uuidv4 } from "uuid";
import { CommonUiModule } from "../../common-ui.module";
import { States } from "../../ngrx-store/states";
import { Service, Websocket } from "../../shared";
import { Language } from "../../type/language";
import de from "./i18n/de.json";
import en from "./i18n/en.json";
import { OAuthService } from "./oauth.service";

@Component({
    selector: "oauth-callback",
    template: `<ion-content class="background">
                    <ion-grid class="ion-full-height">
                        <ion-row class="ion-justify-content-center ion-full-height">
                            <ngx-spinner [name]="spinnerId">
                                <p style="color: white" translate>
                                    <span translate>
                                        AUTHENTICATING
                                    </span>
                                    ...
                                </p>
                            </ngx-spinner>
                        </ion-row>
                    </ion-grid>
                </ion-content>`,
    imports: [
        NgxSpinnerModule,
        CommonUiModule,
        TranslateModule,
    ],
})
export class OAuthCallBackComponent implements OnDestroy, ViewWillLeave {

    protected spinnerId: string = uuidv4();

    constructor(
        private route: ActivatedRoute,
        private oAuthService: OAuthService,
        private websocket: Websocket,
        private service: Service,
        private translate: TranslateService,
    ) {

        Language.normalizeAdditionalTranslationFiles({ de: de, en: en }).then((translations) => {
            for (const { lang, translation, shouldMerge } of translations) {
                translate.setTranslation(lang, translation, shouldMerge);
            }
        });

        const context = effect(async () => {
            const status = this.websocket.state();
            this.service.startSpinner(this.spinnerId, { fullScreen: true });
            if (States.isAtLeast(status, States.WEBSOCKET_CONNECTED)) {
                await OAuthCallBackComponent.processQueryParams(this.route, this.oAuthService);
                context.destroy();
            };
        });
    }

    /**
     * Processes query params.
     *
     * @param route the current route
     * @param oAuthService the oauth service
     * @returns
     */
    public static async processQueryParams(route: ActivatedRoute, oAuthService: OAuthService): Promise<void> {
        const code = route.snapshot.queryParams["code"];

        const oauthState = oAuthService.getOAuthState();
        if (oauthState == null) {
            return;
        }

        await oAuthService.getTokenByCode(code, oauthState);
    }

    ngOnDestroy() {
        this.service.stopSpinner(this.spinnerId);
    }

    ionViewWillLeave() {
        this.ngOnDestroy();
    }
}
