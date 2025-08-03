
import { Component, LOCALE_ID, inject } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { ActivatedRoute, Router } from "@angular/router";
import { IonicModule, ModalController } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { ChartComponentsModule } from "src/app/shared/components/chart/chart.module";
import { NavigationOption } from "src/app/shared/components/footer/subnavigation/footerNavigation";
import { FooterNavigationComponentsModule } from "src/app/shared/components/footer/subnavigation/footerNavigation.module";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { PickdateComponentModule } from "src/app/shared/components/pickdate/pickdate.module";
import { Language } from "src/app/shared/type/language";
import { AbstractHistoryChartOverview } from "../../../../../shared/components/chart/abstractHistoryChartOverview";
import { EdgeConfig, Service } from "../../../../../shared/shared";
import { StorageTotalChartComponent } from "../chart/totalchart";

@Component({
    selector: "storage-chart-overview",
    templateUrl: "./overview.html",
    standalone: true,
    imports: [
    ReactiveFormsModule,
    IonicModule,
    TranslateModule,
    ChartComponentsModule,
    PickdateComponentModule,
    HistoryDataErrorModule,
    StorageTotalChartComponent,
    FooterNavigationComponentsModule
],
    providers: [
        { provide: LOCALE_ID, useFactory: () => (Language.getByKey(localStorage.LANGUAGE) ?? Language.getByBrowserLang(navigator.language) ?? Language.DEFAULT).key },
    ],
})
export class OverviewComponent extends AbstractHistoryChartOverview {
    override service: Service;
    protected override route: ActivatedRoute;
    override modalCtrl: ModalController;
    private router = inject(Router);


    protected essComponents: EdgeConfig.Component[] | null = null;
    protected navigationButtons: NavigationOption[] = [];

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
