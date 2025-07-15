import { CommonModule } from "@angular/common";
import { Component, LOCALE_ID } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { IonicModule, ModalController } from "@ionic/angular";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";
import { ChartComponentsModule } from "src/app/shared/components/chart/chart.module";
import { ChartTypes } from "src/app/shared/components/chart/chart.types";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { PickdateComponentModule } from "src/app/shared/components/pickdate/pickdate.module";
import { Service } from "src/app/shared/shared";
import { Language } from "src/app/shared/type/language";
import { ChartComponent } from "../chart/chart";
@Component({
    selector: "controller-io-heatpump-overview",
    templateUrl: "./overview.html",
    standalone: true,
    imports: [
        ReactiveFormsModule,
        CommonModule,
        IonicModule,
        TranslateModule,
        ChartComponentsModule,
        PickdateComponentModule,
        HistoryDataErrorModule,
        ChartComponent,
    ],
    providers: [
        { provide: LOCALE_ID, useFactory: () => (Language.getByKey(localStorage.LANGUAGE) ?? Language.getByBrowserLang(navigator.language) ?? Language.DEFAULT).key },

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
    }

    protected setChartConfig(event: ChartTypes.ChartConfig) {
        this.chartType = event.chartType;
    }

}
