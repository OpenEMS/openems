import { Component } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";
import { NavigationOption } from "src/app/shared/components/footer/subnavigation/footerNavigation";
import { ChannelAddress, EdgeConfig, Service } from "src/app/shared/shared";

@Component({
    templateUrl: "./overview.html",
})
export class OverviewComponent extends AbstractHistoryChartOverview {

    protected navigationButtons: NavigationOption[] = [];
    protected evcsComponents: EdgeConfig.Component[] = [];
    protected consumptionMeterComponents: EdgeConfig.Component[] = [];

    constructor(
        public override service: Service,
        protected override route: ActivatedRoute,
        public override modalCtrl: ModalController,
        private router: Router,
        private translate: TranslateService,
    ) {
        super(service, route, modalCtrl);
    }

    protected override getChannelAddresses(): ChannelAddress[] {

        this.evcsComponents = this.config?.getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs")
            .filter(component =>
                !(component.factoryId === "Evcs.Cluster.SelfConsumption") &&
                !(component.factoryId === "Evcs.Cluster.PeakShaving") &&
                !component.isEnabled === false);

        this.consumptionMeterComponents = this.config?.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
            .filter(component => component.isEnabled && this.config.isTypeConsumptionMetered(component));

        const sum: EdgeConfig.Component = this.config.getComponent("_sum");
        sum.alias = this.translate.instant("General.TOTAL");

        this.navigationButtons = [sum, ...this.evcsComponents, ...this.consumptionMeterComponents].map(el => (
            { id: el.id, alias: el.alias, callback: () => { this.router.navigate(["./" + el.id], { relativeTo: this.route }); } }
        ));

        return [];
    }
}
