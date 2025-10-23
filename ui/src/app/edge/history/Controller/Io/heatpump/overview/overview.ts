import { Component } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";
import { ChartComponentsModule } from "src/app/shared/components/chart/chart.module";
import { ChartTypes } from "src/app/shared/components/chart/chart.types";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { PickdateComponentModule } from "src/app/shared/components/pickdate/pickdate.module";
import { LocaleProvider } from "src/app/shared/provider/locale-provider";
import { Service } from "src/app/shared/shared";
import { Language } from "src/app/shared/type/language";
import { ChartComponent } from "../chart/chart";
import tr from "./translation.json";
@Component({
    selector: "controller-io-heatpump-overview",
    templateUrl: "./overview.html",
    standalone: true,
    imports: [
        CommonUiModule,
        LocaleProvider,
        ReactiveFormsModule,
        ChartComponent,
        ChartComponentsModule,
        PickdateComponentModule,
        HistoryDataErrorModule,
    ],
})
export class OverviewComponent extends AbstractHistoryChartOverview {

    protected readonly STATES: string = `
    1.${this.translate.instant("Edge.Index.Widgets.HeatPump.lock")}
    2.${this.translate.instant("Edge.Index.Widgets.HeatPump.normalOperation")} 
    3.${this.translate.instant("Edge.Index.Widgets.HeatPump.switchOnRec")} 
    4.${this.translate.instant("Edge.Index.Widgets.HeatPump.switchOnCom")}
    `;
    protected chartType: "line" | "bar" = "line";

    constructor(
        public override service: Service,
        protected override route: ActivatedRoute,
        public override modalCtrl: ModalController,
        private translate: TranslateService,
    ) {
        super(service, route, modalCtrl);
        Language.setAdditionalTranslationFile(tr, this.translate).then(({ lang, translations, shouldMerge }) => {
            this.translate.setTranslation(lang, translations, shouldMerge);
        });
    }

    protected setChartConfig(event: ChartTypes.ChartConfig) {
        this.chartType = event.chartType;
    }

}
