import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { IonicModule, ModalController } from "@ionic/angular";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { NgxSpinnerModule } from "ngx-spinner";

import { UserSettings } from "src/app/shared/jsonrpc/shared";
import { UserService } from "src/app/shared/service/user.service";
import { Service } from "src/app/shared/shared";
import { Language } from "src/app/shared/type/language";
import { StringUtils } from "src/app/shared/utils/string/string.utils";
import { environment } from "src/environments";
import { FlatWidgetButtonComponent } from "../../../flat/flat-widget-button/flat-widget-button";
import { ModalComponentsModule } from "../../../modal/modal.module";
import { OeCheckboxComponent } from "../../../oe-checkbox/oe-checkbox";
import { OeImageComponent } from "../../../oe-img/oe-img";
import { Edge } from "../../edge";
import de from "../shared/i18n/de.json";
import en from "../shared/i18n/en.json";
import { MarketingAnnualReviewButtonComponent } from "./button/button";

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
        FlatWidgetButtonComponent,
        MarketingAnnualReviewButtonComponent,
    ],
})
export class MarketingAnnualReviewComponent implements OnInit {
    protected img: OeImageComponent["img"] | null = null;
    protected link: FlatWidgetButtonComponent["link"] | null = null;

    constructor(
        protected modalCtrl: ModalController,
        private translate: TranslateService,
        private service: Service,
        private userService: UserService,
    ) {
        Language.normalizeAdditionalTranslationFiles({ de: de, en: en }).then((translations) => {
            for (const { lang, translation, shouldMerge } of translations) {
                translate.setTranslation(lang, translation, shouldMerge);
            }
        });
    }

    public static shouldShowPopover(edge: Edge, userService: UserService) {
        return edge.shouldShowAnnualReviewPopover()
            && StringUtils.isNotInArr(edge.id, userService.currentUser()?.getAnnualReviewFromSettings() ?? [])
            && environment.theme === "FENECON";
    }

    private static PREFIX: (edge: Edge | null) => string = (edge: Edge | null) => edge == null ? "" : "popover-annual-review-" + edge.id;

    ngOnInit() {
        if (environment.theme !== "FENECON") {
            return;
        }
        this.img = {
            url: "assets/img/annual-review.svg",
            width: this.service.isSmartphoneResolution ? 70 : 40,
        };
    }

    protected async dismiss() {
        const edge = await this.service.getCurrentEdge();
        const user = this.userService.currentUser();
        this.userService.updateUserSettingsWithProperty(UserSettings.ANNUAL_REVIEW, Array.from(new Set([...user?.getAnnualReviewFromSettings() ?? [], edge.id])));
        localStorage.setItem(MarketingAnnualReviewComponent.PREFIX(edge), "true");
        this.modalCtrl.dismiss();
    }
}
