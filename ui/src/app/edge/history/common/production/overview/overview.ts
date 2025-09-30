import { Component } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { NavigationOption } from "src/app/shared/components/footer/subnavigation/footerNavigation";
import { AbstractHistoryChartOverview } from "../../../../../shared/components/chart/abstractHistoryChartOverview";
import { ChannelAddress, EdgeConfig, Service } from "../../../../../shared/shared";

@Component({
  templateUrl: "./OVERVIEW.HTML",
  standalone: false,
})
export class OverviewComponent extends AbstractHistoryChartOverview {
  protected chargerComponents: EDGE_CONFIG.COMPONENT[] = [];
  protected productionMeterComponents: EDGE_CONFIG.COMPONENT[] = [];
  protected navigationButtons: NavigationOption[] = [];

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
    //  Get Chargers
    THIS.CHARGER_COMPONENTS =
      THIS.CONFIG.GET_COMPONENTS_IMPLEMENTING_NATURE("IO.OPENEMS.EDGE.ESS.DCCHARGER.API.ESS_DC_CHARGER")
        .filter(component => COMPONENT.IS_ENABLED);

    // Get productionMeters
    THIS.PRODUCTION_METER_COMPONENTS =
      THIS.CONFIG.GET_COMPONENTS_IMPLEMENTING_NATURE("IO.OPENEMS.EDGE.METER.API.ELECTRICITY_METER")
        .filter(component => COMPONENT.IS_ENABLED && THIS.CONFIG.IS_PRODUCER(component));


    THIS.NAVIGATION_BUTTONS = [...THIS.PRODUCTION_METER_COMPONENTS, ...THIS.CHARGER_COMPONENTS].map(el => (
      { id: EL.ID, alias: EL.ALIAS, callback: () => { THIS.ROUTER.NAVIGATE(["./" + EL.ID], { relativeTo: THIS.ROUTE }); } }
    ));
    return [];
  }
}
