import { Component } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { ActivatedRoute, Router } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ChartComponentsModule } from "src/app/shared/components/chart/chart.module";
import { NavigationOption } from "src/app/shared/components/footer/subnavigation/footerNavigation";
import { FooterNavigationComponentsModule } from "src/app/shared/components/footer/subnavigation/footerNavigation.module";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { PickdateComponentModule } from "src/app/shared/components/pickdate/pickdate.module";
import { LocaleProvider } from "src/app/shared/provider/locale-provider";
import { AbstractHistoryChartOverview } from "../../../../../shared/components/chart/abstractHistoryChartOverview";
import { EdgeConfig, Service } from "../../../../../shared/shared";
import { StorageTotalChartComponent } from "../chart/totalchart";

@Component({
    selector: "storage-chart-overview",
    templateUrl: "./overview.html",
    standalone: true,
    imports: [
        CommonUiModule,
        LocaleProvider,
        ReactiveFormsModule,
        ChartComponentsModule,
        PickdateComponentModule,
        HistoryDataErrorModule,
        StorageTotalChartComponent,
        FooterNavigationComponentsModule,
    ],
})
export class OverviewComponent extends AbstractHistoryChartOverview {

    protected essComponents: EdgeConfig.Component[] | null = null;
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
        this.essComponents =
            this.config?.getComponentsImplementingNature("io.openems.edge.ess.api.SymmetricEss")
                .filter(component => component.isEnabled && !component.factoryId.includes("Ess.Cluster"));

        if (!this.essComponents || this.essComponents.length <= 1) {
            return;
        }

        this.navigationButtons = this.essComponents.map(el => (
            { id: el.id, alias: el.alias, callback: () => { this.router.navigate(["./" + el.id], { relativeTo: this.route }); } }
        ));
    }
}
