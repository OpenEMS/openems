
import { Component, LOCALE_ID, inject } from "@angular/core";
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
import tr from "./translation.json";
@Component({
    selector: "controller-io-heatpump-overview",
    templateUrl: "./overview.html",
    standalone: true,
    imports: [
    ReactiveFormsModule,
    IonicModule,
    TranslateModule,
    ChartComponentsModule,
    PickdateComponentModule,
    HistoryDataErrorModule,
    ChartComponent
],
    providers: [
        { provide: LOCALE_ID, useFactory: () => (Language.getByKey(localStorage.LANGUAGE) ?? Language.getByBrowserLang(navigator.language) ?? Language.DEFAULT).key },

    ],
})
export class OverviewComponent extends AbstractHistoryChartOverview {
    override service: Service;
    protected override route: ActivatedRoute;
    override modalCtrl: ModalController;
    private translate = inject(TranslateService);


    protected readonly STATES: string = `
    1.${this.translate.instant("Edge.Index.Widgets.HeatPump.lock")}
    2.${this.translate.instant("Edge.Index.Widgets.HeatPump.normalOperation")} 
    3.${this.translate.instant("Edge.Index.Widgets.HeatPump.switchOnRec")} 
    4.${this.translate.instant("Edge.Index.Widgets.HeatPump.switchOnCom")}
    `;
    protected chartType: "line" | "bar" = "line";

    /** Inserted by Angular inject() migration for backwards compatibility */
    constructor(...args: unknown[]);

    constructor() {
        const service = inject(Service);
        const route = inject(ActivatedRoute);
        const modalCtrl = inject(ModalController);

        super(service, route, modalCtrl);
        this.service = service;
        this.route = route;
        this.modalCtrl = modalCtrl;

        Language.setAdditionalTranslationFile(tr, this.translate).then(({ lang, translations, shouldMerge }) => {
            this.translate.setTranslation(lang, translations, shouldMerge);
        });
    }

    protected setChartConfig(event: ChartTypes.ChartConfig) {
        this.chartType = event.chartType;
    }

}
