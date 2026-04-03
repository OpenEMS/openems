import { Component } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";
import { ChartComponentsModule } from "src/app/shared/components/chart/chart.module";
import { ChartTypes } from "src/app/shared/components/chart/chart.types";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { PickdateComponentModule } from "src/app/shared/components/pickdate/pickdate.module";
import { LocaleProvider } from "src/app/shared/provider/locale-provider";
import { Service } from "src/app/shared/shared";
import { Language } from "src/app/shared/type/language";
import { SharedControllerIoHeatpump } from "../../shared/shared";
import { ChartComponent } from "../chart/chart";
import de from "../shared/i18n/de.json";
import en from "../shared/i18n/en.json";

@Component({
    selector: "controller-io-heatpump-overview",
    templateUrl: "./new-navigation.html",
    standalone: true,
    imports: [
        CommonUiModule,
        LocaleProvider,
        ReactiveFormsModule,
        ChartComponent,
        ChartComponentsModule,
        PickdateComponentModule,
        HistoryDataErrorModule,
        ComponentsBaseModule,
    ],
})
export class ControllerIoHeatpumpHistoryComponent extends AbstractHistoryChartOverview {

    protected readonly STATES = SharedControllerIoHeatpump.getHeatPumpStates(this.translate);
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
