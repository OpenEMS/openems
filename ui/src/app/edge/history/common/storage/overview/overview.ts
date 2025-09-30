import { CommonModule } from "@angular/common";
import { Component, LOCALE_ID } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { ActivatedRoute, Router } from "@angular/router";
import { IonicModule, ModalController } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { ChartComponentsModule } from "src/app/shared/components/chart/CHART.MODULE";
import { NavigationOption } from "src/app/shared/components/footer/subnavigation/footerNavigation";
import { FooterNavigationComponentsModule } from "src/app/shared/components/footer/subnavigation/FOOTER_NAVIGATION.MODULE";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-ERROR.MODULE";
import { PickdateComponentModule } from "src/app/shared/components/pickdate/PICKDATE.MODULE";
import { Language } from "src/app/shared/type/language";
import { AbstractHistoryChartOverview } from "../../../../../shared/components/chart/abstractHistoryChartOverview";
import { EdgeConfig, Service } from "../../../../../shared/shared";
import { StorageTotalChartComponent } from "../chart/totalchart";

@Component({
    selector: "storage-chart-overview",
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
        StorageTotalChartComponent,
        FooterNavigationComponentsModule,
    ],
    providers: [
        { provide: LOCALE_ID, useFactory: () => (LANGUAGE.GET_BY_KEY(LOCAL_STORAGE.LANGUAGE) ?? LANGUAGE.GET_BY_BROWSER_LANG(NAVIGATOR.LANGUAGE) ?? LANGUAGE.DEFAULT).key },
    ],
})
export class OverviewComponent extends AbstractHistoryChartOverview {

    protected essComponents: EDGE_CONFIG.COMPONENT[] | null = null;
    protected navigationButtons: NavigationOption[] = [];

    constructor(
        public override service: Service,
        protected override route: ActivatedRoute,
        public override modalCtrl: ModalController,
        private router: Router,
    ) {
        super(service, route, modalCtrl);
    }

    protected override afterIsInitialized() {
        // Get Ess
        THIS.ESS_COMPONENTS =
            THIS.CONFIG?.getComponentsImplementingNature("IO.OPENEMS.EDGE.ESS.API.SYMMETRIC_ESS")
                .filter(component => COMPONENT.IS_ENABLED && !COMPONENT.FACTORY_ID.INCLUDES("ESS.CLUSTER"));

        if (!THIS.ESS_COMPONENTS || THIS.ESS_COMPONENTS.LENGTH <= 1) {
            return;
        }

        THIS.NAVIGATION_BUTTONS = THIS.ESS_COMPONENTS.MAP(el => (
            { id: EL.ID, alias: EL.ALIAS, callback: () => { THIS.ROUTER.NAVIGATE(["./" + EL.ID], { relativeTo: THIS.ROUTE }); } }
        ));
    }
}
