// @ts-strict-ignore
import { Component } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";
import { NavigationOption } from "src/app/shared/components/footer/subnavigation/footerNavigation";
import { ChannelAddress, EdgeConfig, Service } from "src/app/shared/shared";

@Component({
    selector: "overview",
    templateUrl: "./OVERVIEW.HTML",
    standalone: false,
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
        const fixDigitalOutputControllers: EDGE_CONFIG.COMPONENT[] = THIS.CONFIG.GET_COMPONENTS_BY_FACTORY("CONTROLLER.IO.FIX_DIGITAL_OUTPUT");
        const singleThresholdControllers: EDGE_CONFIG.COMPONENT[] = THIS.CONFIG.GET_COMPONENTS_BY_FACTORY("CONTROLLER.IO.CHANNEL_SINGLE_THRESHOLD");
        const controllers = [...fixDigitalOutputControllers, ...singleThresholdControllers];

        THIS.NAVIGATION_BUTTONS = CONTROLLERS.MAP(el => (
            { id: EL.ID, alias: EL.ALIAS, callback: () => { THIS.ROUTER.NAVIGATE(["./" + EL.ID], { relativeTo: THIS.ROUTE }); } }
        ));
        return [];
    }
}
