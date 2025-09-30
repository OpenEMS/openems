import { CommonModule } from "@angular/common";
import { Component, LOCALE_ID } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { IonicModule, ModalController } from "@ionic/angular";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";
import { ChartComponentsModule } from "src/app/shared/components/chart/CHART.MODULE";
import { ChartTypes } from "src/app/shared/components/chart/CHART.TYPES";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-ERROR.MODULE";
import { PickdateComponentModule } from "src/app/shared/components/pickdate/PICKDATE.MODULE";
import { Service } from "src/app/shared/shared";
import { Language } from "src/app/shared/type/language";
import { ChartComponent } from "../chart/chart";
import tr from "./TRANSLATION.JSON";
@Component({
    selector: "controller-io-heatpump-overview",
    templateUrl: "./OVERVIEW.HTML",
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
        { provide: LOCALE_ID, useFactory: () => (LANGUAGE.GET_BY_KEY(LOCAL_STORAGE.LANGUAGE) ?? LANGUAGE.GET_BY_BROWSER_LANG(NAVIGATOR.LANGUAGE) ?? LANGUAGE.DEFAULT).key },

    ],
})
export class OverviewComponent extends AbstractHistoryChartOverview {

    protected readonly STATES: string = `
    1.${THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.HEAT_PUMP.LOCK")}
    2.${THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.HEAT_PUMP.NORMAL_OPERATION")} 
    3.${THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.HEAT_PUMP.SWITCH_ON_REC")} 
    4.${THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.HEAT_PUMP.SWITCH_ON_COM")}
    `;
    protected chartType: "line" | "bar" = "line";

    constructor(
        public override service: Service,
        protected override route: ActivatedRoute,
        public override modalCtrl: ModalController,
        private translate: TranslateService,
    ) {
        super(service, route, modalCtrl);
        LANGUAGE.SET_ADDITIONAL_TRANSLATION_FILE(tr, THIS.TRANSLATE).then(({ lang, translations, shouldMerge }) => {
            THIS.TRANSLATE.SET_TRANSLATION(lang, translations, shouldMerge);
        });
    }

    protected setChartConfig(event: CHART_TYPES.CHART_CONFIG) {
        THIS.CHART_TYPE = EVENT.CHART_TYPE;
    }

}
