// @ts-strict-ignore
import { ChangeDetectionStrategy, Component, effect, inject, OnDestroy } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { ViewWillLeave } from "@ionic/angular";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { NgxSpinnerModule } from "ngx-spinner";
import { v4 as uuidv4 } from "uuid";
import { CommonUiModule } from "../../common-ui.module";
import { Service } from "../../shared";
import { States } from "../../states/states";
import { Language } from "../../type/language";
import cz from "./i18n/cz.json";
import de from "./i18n/de.json";
import en from "./i18n/en.json";
import es from "./i18n/es.json";
import fr from "./i18n/fr.json";
import ja from "./i18n/ja.json";
import nl from "./i18n/nl.json";
import { OAuthService } from "./oauth.service";

@Component({
    selector: "oauth-callback",
    template: `<ion-content class="background">
        <ion-grid class="ion-full-height">
            <ion-row class="ion-justify-content-center ion-full-height">
                <ngx-spinner [name]="spinnerId">
                    <p style="color: white" translate>
                        <span translate> AUTHENTICATING </span>
                        ...
                    </p>
                </ngx-spinner>
            </ion-row>
        </ion-grid>
    </ion-content>`,
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [NgxSpinnerModule, CommonUiModule, TranslateModule],
})
export class OAuthCallBackComponent implements OnDestroy, ViewWillLeave {
    public static readonly ID: string = "oauthcallback";
    protected spinnerId: string = uuidv4();
    private oauthService: OAuthService = inject(OAuthService);

    constructor(
        private route: ActivatedRoute,
        private service: Service,
        private translate: TranslateService,
    ) {
        Language.normalizeAdditionalTranslationFiles({ cs: cz, de: de, en: en, es: es, fr: fr, ja: ja, nl: nl }).then(
            (translations) => {
                for (const { lang, translation, shouldMerge } of translations) {
                    translate.setTranslation(lang, translation, shouldMerge);
                }
            },
        );

        effect(async () => {
            const status = this.service.websocket.state();
            this.service.startSpinner(this.spinnerId, { fullScreen: true });
            if (States.isAtLeast(status, States.WEBSOCKET_CONNECTED)) {
                await OAuthCallBackComponent.processQueryParams(this.route, this.oauthService);
            }
        });
    }

    /**
     * Processes query params.
     *
     * @param route The current route
     * @param oAuthService The oauth service
     * @returns
     */
    public static async processQueryParams(route: ActivatedRoute, oAuthService: OAuthService): Promise<void> {
        const code = route.snapshot.queryParams["code"];

        const oauthState = oAuthService.getOAuthState();
        if (oauthState == null) {
            return;
        }

        oAuthService.getTokenByCode(code, oauthState);
    }

    ngOnDestroy() {
        this.service.stopSpinner(this.spinnerId);
    }

    ionViewWillLeave() {
        this.ngOnDestroy();
    }
}
