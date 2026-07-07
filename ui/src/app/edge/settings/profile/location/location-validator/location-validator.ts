import { Component, inject, OnInit } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { MetaComponent } from "src/app/shared/components/edge/config-components/meta/meta";
import { SystemLocationValidatorComponent } from "src/app/shared/components/system-location-validator/system-location-validator.component";
import { Edge, EdgeConfig, Service } from "src/app/shared/shared";
import { Language } from "src/app/shared/type/language";
import { PromiseUtils } from "src/app/shared/utils/promise/promise.utils";
import de from "../i18n/de.json";
import en from "../i18n/en.json";

@Component({
    selector: "oe-settings-profile-location-validator",
    template: `
    @if(coordinates !== null){
        <ion-row>
            <ion-col>
                <ion-card>
                    <ion-item color="light" lines="full">
                        <ion-icon slot="start" name="location-outline" color="primary"></ion-icon>
                        <ion-label>
                            <ion-card-title class="ion-text-wrap" style="color: var(--ion-text-color)" translate>SETTINGS_PROFILE_LOCATION.EDIT_CARD_TITLE</ion-card-title>
                            <ion-card-subtitle class="ion-text-wrap" translate>SETTINGS_PROFILE_LOCATION.EDIT_CARD_SUBTITLE</ion-card-subtitle>
                            <ion-card-subtitle class="ion-text-wrap" translate>SETTINGS_PROFILE_LOCATION.EDIT_CARD_INFO</ion-card-subtitle>
                            @if(hasValidatedCoordinates === true){
                                <ion-card-subtitle style="color: var(--ion-color-success)" class="ion-padding-top" translate>SETTINGS_PROFILE_LOCATION.ADRESS_IS_VALIDATED</ion-card-subtitle>
                            }
                        </ion-label>
                    </ion-item>
                    <ion-card-content>
                        @if(hasValidatedCoordinates === true){
                            <ion-item>
                                <ion-input [label]="('SETTINGS_PROFILE_LOCATION.LONGITUDE' | translate)" [disabled]="true">
                                    <ion-text slot="end">{{coordinates.longitude}}</ion-text>
                                </ion-input>
                            </ion-item>
                            <ion-item>
                                <ion-input [label]="('SETTINGS_PROFILE_LOCATION.LATITUDE' | translate)" [disabled]="true">
                                    <ion-text slot="end">{{coordinates.latitude}}</ion-text>
                                </ion-input>
                            </ion-item>
                        }
                        <system-location-validator context="PROFILE" (addressUpdated)="onAddressUpdated()"></system-location-validator>
                    </ion-card-content>
                </ion-card>
            </ion-col>
        </ion-row>
    }
    `,
    imports: [
        CommonUiModule,
        SystemLocationValidatorComponent,
    ],
})
export class LocationValidatorComponent implements OnInit {

    protected edge: Edge | null = null;
    protected coordinates: ReturnType<MetaComponent["getCoordinates"]> | null = null;
    protected hasValidatedCoordinates: boolean = false;
    private service: Service = inject(Service);
    private router: Router = inject(Router);
    private route: ActivatedRoute = inject(ActivatedRoute);
    private translate: TranslateService = inject(TranslateService);

    public async ngOnInit() {
        Language.normalizeAdditionalTranslationFiles({ de: de, en: en }).then((translations) => {
            for (const { lang, translation, shouldMerge } of translations) {
                this.translate.setTranslation(lang, translation, shouldMerge);
            }
        });

        const [err, config] = await PromiseUtils.Functions.handle<EdgeConfig>(this.service.getConfig());

        if (err) {
            return;
        }

        const meta = new MetaComponent(config);
        this.coordinates = meta.getCoordinates();
        this.hasValidatedCoordinates = meta.hasValidCoordinates();
    }

    protected onAddressUpdated() {
        this.service.toast(this.translate.instant("SETTINGS_PROFILE_LOCATION.SUCCESSFULL_VALIDATION"), "success");
        this.router.navigate(["../"], { relativeTo: this.route });
    }

}
