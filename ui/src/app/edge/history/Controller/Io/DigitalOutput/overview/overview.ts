// @ts-strict-ignore
import { Component } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";
import { NavigationOption } from "src/app/shared/components/footer/subnavigation/footerNavigation";
import { ChannelAddress, EdgeConfig, Service } from "src/app/shared/shared";

@Component({
    selector: "overview",
    templateUrl: "./overview.html",
})
export class OverviewComponent extends AbstractHistoryChartOverview {

    protected navigationButtons: NavigationOption[] = [];

    constructor(
        public override service: Service,
        protected override route: ActivatedRoute,
        public override modalCtrl: ModalController,
        private router: Router,
    ) {
        super(service, route, modalCtrl);
    }

    protected override getChannelAddresses(): ChannelAddress[] {
        const fixDigitalOutputControllers: EdgeConfig.Component[] = this.config.getComponentsByFactory("Controller.Io.FixDigitalOutput");
        const singleThresholdControllers: EdgeConfig.Component[] = this.config.getComponentsByFactory("Controller.IO.ChannelSingleThreshold");
        const controllers = [...fixDigitalOutputControllers, ...singleThresholdControllers];

        this.navigationButtons = controllers.map(el => (
            { id: el.id, alias: el.alias, callback: () => { this.router.navigate(["./" + el.id], { relativeTo: this.route }); } }
        ));
        return [];
    }
}
