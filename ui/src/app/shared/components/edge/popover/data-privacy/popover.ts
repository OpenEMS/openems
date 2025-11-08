import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormBuilder, FormControl, FormGroup, Validators } from "@angular/forms";
import { IonicModule, ModalController } from "@ionic/angular";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { NgxSpinnerModule } from "ngx-spinner";
import { v4 as uuidv4 } from "uuid";

import { WeatherIcon } from "src/app/edge/live/common/weather/models/weather-icon";
import { AddAppInstance } from "src/app/edge/settings/app/jsonrpc/addAppInstance";
import { AppCenter } from "src/app/edge/settings/app/keypopup/appCenter";
import { AppCenterInstallAppWithSuppliedKeyRequest } from "src/app/edge/settings/app/keypopup/appCenterInstallAppWithSuppliedKey";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { Edge, Service } from "src/app/shared/shared";
import { Language } from "src/app/shared/type/language";
import { DocsUtils } from "src/app/shared/utils/docs/docs.utils";
import { FormUtils } from "src/app/shared/utils/form/form.utils";
import { PromiseUtils } from "src/app/shared/utils/promise/promise.utils";
import { ModalComponentsModule } from "../../../modal/modal.module";
import { OeCheckboxComponent } from "../../../oe-checkbox/oe-checkbox";
import { OeImageComponent } from "../../../oe-img/oe-img";
import de from "../shared/i18n/de.json";
import en from "../shared/i18n/en.json";
import { ThirdPartyUsageAcceptance } from "../shared/third-party-usage-acceptance";

@Component({
    selector: "oe-weather-forecast-approval-popover",
    templateUrl: "./popover.html",
    standalone: true,
    imports: [
        CommonModule,
        IonicModule,
        ModalComponentsModule,
        TranslateModule,
        OeCheckboxComponent,
        NgxSpinnerModule,
    ],
})
export class WeatherForecastApprovalComponent implements OnInit {
    protected formGroup: FormGroup | null = null;
    protected img: OeImageComponent["img"] = {
        url: "/assets/img/weather-popover-app-img.svg",
        width: 50,
        style: "justify-self: center;",
    };
    protected dataProtectionLink: string | null = null;
    protected spinnerId: string = uuidv4();

    protected spinnerText: string | null = null;
    protected spinnerIcon: WeatherIcon | null = null;

    constructor(
        protected modalCtrl: ModalController,
        private formBuilder: FormBuilder,
        private translate: TranslateService,
        private service: Service,
    ) {
        Language.normalizeAdditionalTranslationFiles({ de: de, en: en }).then((translations) => {
            for (const { lang, translation, shouldMerge } of translations) {
                translate.setTranslation(lang, translation, shouldMerge);
            }
        });
    }

    ngOnInit() {
        this.dataProtectionLink = DocsUtils.createDataProtectionLink(this.service);
        this.formGroup = this.formBuilder.group({
            isChecked: new FormControl(false, Validators.required),
        });
    }

    protected async apply() {
        if (this.formGroup == null) {
            return;
        }
        const isChecked = FormUtils.findFormControlsValueSafely<boolean>(this.formGroup, "isChecked");
        const edge = await this.service.getCurrentEdge();

        if (isChecked == false || edge == null) {
            return;
        }

        this.spinnerText = this.translate.instant("POPOVER.WEATHER_PROGNOSIS.SPINNER_TEXT_ACCEPTED");
        this.spinnerIcon = WeatherIcon.ClearDay;
        this.service.startSpinner(this.spinnerId);

        let installSuccess = false;

        try {
            const updated = await this.updateMetaApp(edge, ThirdPartyUsageAcceptance.ACCEPTED);
            if (updated == false) {
                return;
            }

            const installed = await this.installWeatherApp(edge);
            if (installed == false) {
                return;
            }

            installSuccess = true;
        } finally {
            this.service.stopSpinner(this.spinnerId);
            this.modalCtrl.dismiss();
        }

        if (installSuccess == true) {
            this.service.toast(
                this.translate.instant("EDGE.CONFIG.APP.SUCCESS_INSTALL"),
                "success"
            );

            await new Promise(resolve => setTimeout(resolve, 2000));
            window.location.reload();
        }
    }

    protected async decline() {
        const edge = await this.service.getCurrentEdge();
        if (edge == null) {
            return;
        }

        this.spinnerText = this.translate.instant("POPOVER.WEATHER_PROGNOSIS.SPINNER_TEXT_DECLINED");
        this.spinnerIcon = WeatherIcon.WeatherRainy;
        this.service.startSpinner(this.spinnerId);

        try {
            await this.updateMetaApp(edge, ThirdPartyUsageAcceptance.DECLINED);
        } finally {
            this.service.stopSpinner(this.spinnerId);
            this.modalCtrl.dismiss();
        }
    }

    protected async dismiss() {
        this.modalCtrl.dismiss();
    }

    protected setFormGroup(event: FormGroup) {
        this.formGroup = event;
    }

    /**
     * Updates the meta app
     *
     * @param edge the current edge
     * @param thirdPartyUsageAcceptance third party usage acceptance state
     * @returns success
     */
    private async updateMetaApp(edge: Edge, thirdPartyUsageAcceptance: ThirdPartyUsageAcceptance): Promise<boolean> {
        const [err] = await PromiseUtils.Functions.handle(
            edge.updateAppConfig(this.service.websocket, "_meta", [
                { name: "thirdPartyUsageAcceptance", value: thirdPartyUsageAcceptance },
            ])
        );

        if (err) {
            this.service.toast(
                this.translate.instant("EDGE.CONFIG.APP.FAIL_UPDATE", { error: err.message || err }),
                "danger"
            );
            return false;
        }

        return true;
    }

    /**
     * Installs the weather app
     *
     * @param edge the current edge
     * @returns success
     */
    private async installWeatherApp(edge: Edge): Promise<boolean> {
        const request = new AppCenter.Request({
            payload: new AppCenterInstallAppWithSuppliedKeyRequest.Request({
                installRequest: new ComponentJsonApiRequest({
                    componentId: "_appManager",
                    payload: new AddAppInstance.Request({
                        appId: "App.Prediction.Weather",
                        alias: this.translate.instant("POPOVER.WEATHER_PROGNOSIS.APP_WEATHER"),
                        properties: {},
                    }),
                }),
            }),
        });

        const [err] = await PromiseUtils.Functions.handle(
            edge.sendRequest(this.service.websocket, request)
        );

        if (err) {
            this.service.toast(
                this.translate.instant("EDGE.CONFIG.APP.FAIL_INSTALL", { error: err.message || err }),
                "danger"
            );
            return false;
        }

        return true;
    }
}
