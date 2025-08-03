import { Component, inject } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";
import { NavigationOption } from "src/app/shared/components/footer/subnavigation/footerNavigation";
import { ChannelAddress, EdgeConfig, Service } from "src/app/shared/shared";

@Component({
    templateUrl: "./overview.html",
    standalone: false,
})
export class OverviewComponent extends AbstractHistoryChartOverview {
    override service: Service;
    protected override route: ActivatedRoute;
    override modalCtrl: ModalController;
    private router = inject(Router);
    private translate = inject(TranslateService);


    protected navigationButtons: NavigationOption[] = [];
    protected evcsComponents: EdgeConfig.Component[] = [];
    protected consumptionMeterComponents: EdgeConfig.Component[] = [];

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

    protected override getChannelAddresses(): ChannelAddress[] {

        this.evcsComponents = this.config?.getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs")
            .filter(component =>
                !(component.factoryId === "Evcs.Cluster.SelfConsumption") &&
                !(component.factoryId === "Evcs.Cluster.PeakShaving") &&
                !component.isEnabled === false);

        const heatComponents = this.config?.getComponentsImplementingNature("io.openems.edge.heat.api.Heat")
            .filter(component =>
                !(component.factoryId === "Controller.Heat.Heatingelement") &&
                !component.isEnabled === false);

        this.consumptionMeterComponents = this.config?.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
            .filter(component => component.isEnabled && this.config.isTypeConsumptionMetered(component)
                && !this.config.getNatureIdsByFactoryId(component.factoryId).includes("io.openems.edge.evcs.api.Evcs")
                && !this.config.getNatureIdsByFactoryId(component.factoryId).includes("io.openems.edge.heat.api.Heat"));

        const sum: EdgeConfig.Component = this.config.getComponent("_sum");
        sum.alias = this.translate.instant("Edge.History.PHASE_ACCURATE");

        this.navigationButtons = [sum, ...this.evcsComponents, ...heatComponents, ...this.consumptionMeterComponents].map(el => (
            { id: el.id, alias: el.alias, callback: () => { this.router.navigate(["./" + el.id], { relativeTo: this.route }); } }
        ));

        return [];
    }
}
