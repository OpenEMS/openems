import { Component } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";
import { ChartTypes } from "src/app/shared/components/chart/chart.types";
import { Service } from "src/app/shared/shared";
import { Language } from "src/app/shared/type/language";
import de from "./i18n/de.json";
import en from "./i18n/en.json";

@Component({
    selector: "enerixControl-overview",
    templateUrl: "./overview.html",
    standalone: false,
})
export class OverviewComponent extends AbstractHistoryChartOverview {
    protected readonly STATES: string = `
    1.${this.translate.instant("EDGE.INDEX.WIDGETS.ENERIX_CONTROL.NO_INPUT")}
    2.${this.translate.instant("EDGE.INDEX.WIDGETS.ENERIX_CONTROL.NO_DISCHARGE")} 
    `;

    // disabled till next release
    // 3.${this.translate.instant("EDGE.INDEX.WIDGETS.ENERIX_CONTROL.FORCE_CHARGE")}

    protected chartType: "line" | "bar" = "line";

    constructor(
        public override service: Service,
        protected override route: ActivatedRoute,
        public override modalCtrl: ModalController,
        private translate: TranslateService,
    ) {
        super(service, route, modalCtrl);
        Language.normalizeAdditionalTranslationFiles({ de: de, en: en }).then((translations) => {
            for (const { lang, translation, shouldMerge } of translations) {
                translate.setTranslation(lang, translation, shouldMerge);
            }
        });
    }

    protected setChartConfig(event: ChartTypes.ChartConfig) {
        this.chartType = event.chartType;
    }
}
