import { Component } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";
import { NavigationOption } from "src/app/shared/components/footer/subnavigation/footerNavigation";
import { ChannelAddress, EdgeConfig, Service } from "src/app/shared/shared";

@Component({
    templateUrl: "./OVERVIEW.HTML",
    standalone: false,
})
export class OverviewComponent extends AbstractHistoryChartOverview {

    protected navigationButtons: NavigationOption[] = [];
    protected evcsComponents: EDGE_CONFIG.COMPONENT[] = [];
    protected consumptionMeterComponents: EDGE_CONFIG.COMPONENT[] = [];

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

        THIS.EVCS_COMPONENTS = THIS.CONFIG?.getComponentsImplementingNature("IO.OPENEMS.EDGE.EVCS.API.EVCS")
            .filter(component =>
                !(COMPONENT.FACTORY_ID === "EVCS.CLUSTER.SELF_CONSUMPTION") &&
                !(COMPONENT.FACTORY_ID === "EVCS.CLUSTER.PEAK_SHAVING") &&
                !COMPONENT.IS_ENABLED === false);

        const heatComponents = THIS.CONFIG?.getComponentsImplementingNature("IO.OPENEMS.EDGE.HEAT.API.HEAT")
            .filter(component =>
                !(COMPONENT.FACTORY_ID === "CONTROLLER.HEAT.HEATINGELEMENT") &&
                !COMPONENT.IS_ENABLED === false);

        THIS.CONSUMPTION_METER_COMPONENTS = THIS.CONFIG?.getComponentsImplementingNature("IO.OPENEMS.EDGE.METER.API.ELECTRICITY_METER")
            .filter(component => COMPONENT.IS_ENABLED && THIS.CONFIG.IS_TYPE_CONSUMPTION_METERED(component)
                && !THIS.CONFIG.GET_NATURE_IDS_BY_FACTORY_ID(COMPONENT.FACTORY_ID).includes("IO.OPENEMS.EDGE.EVCS.API.EVCS")
                && !THIS.CONFIG.GET_NATURE_IDS_BY_FACTORY_ID(COMPONENT.FACTORY_ID).includes("IO.OPENEMS.EDGE.HEAT.API.HEAT"));

        const sum: EDGE_CONFIG.COMPONENT = THIS.CONFIG.GET_COMPONENT("_sum");
        SUM.ALIAS = THIS.TRANSLATE.INSTANT("EDGE.HISTORY.PHASE_ACCURATE");

        THIS.NAVIGATION_BUTTONS = [sum, ...THIS.EVCS_COMPONENTS, ...heatComponents, ...THIS.CONSUMPTION_METER_COMPONENTS].map(el => (
            { id: EL.ID, alias: EL.ALIAS, callback: () => { THIS.ROUTER.NAVIGATE(["./" + EL.ID], { relativeTo: THIS.ROUTE }); } }
        ));

        return [];
    }
}
