import { Component, effect, inject } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { NgxSpinnerModule } from "ngx-spinner";
import { v4 as uuidv4 } from "uuid";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { MetaComponent } from "src/app/shared/components/edge/config-components/meta/meta";
import { EdgeConfig, Service } from "src/app/shared/shared";
import { Language } from "src/app/shared/type/language";
import de from "./i18n/de.json";
import en from "./i18n/en.json";

@Component({
    selector: "oe-settings-profile-location",
    templateUrl: "./location.html",
    imports: [
        CommonUiModule,
        NgxSpinnerModule,
    ],
})
export class LocationComponent {

    protected isValidated: boolean | null = null;
    protected readonly spinnerId: string = uuidv4();

    private service: Service = inject(Service);
    private router: Router = inject(Router);
    private route: ActivatedRoute = inject(ActivatedRoute);

    constructor(translate: TranslateService) {
        Language.normalizeAdditionalTranslationFiles({ de: de, en: en }).then((translations) => {
            for (const { lang, translation, shouldMerge } of translations) {
                translate.setTranslation(lang, translation, shouldMerge);
            }
        });

        effect(() => {
            const edge = this.service.currentEdge();
            const config = edge.getConfigSignal();
            this.init(config());
        });
    }

    public init(config: EdgeConfig) {
        this.service.startSpinner(this.spinnerId);
        this.setLocationValidateState(config).finally(() => this.service.stopSpinner(this.spinnerId));
    }


    protected editLocation() {
        this.router.navigate(["./location-validation"], { relativeTo: this.route });
    }

    private async setLocationValidateState(config: EdgeConfig) {
        return new Promise<void>((resolve, reject) => {
            if (config == null) {
                this.isValidated = false;
                resolve();
            }

            const meta = new MetaComponent(config);
            this.isValidated = meta.hasValidCoordinates();
            resolve();
        });
    }
}
