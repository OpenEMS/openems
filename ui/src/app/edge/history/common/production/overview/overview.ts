import { Component } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { NavigationOption } from "src/app/shared/components/footer/subnavigation/footerNavigation";
import { AbstractHistoryChartOverview } from "../../../../../shared/components/chart/abstractHistoryChartOverview";
import { ChannelAddress, EdgeConfig, Service } from "../../../../../shared/shared";

@Component({
  templateUrl: "./overview.html",
})
export class OverviewComponent extends AbstractHistoryChartOverview {
  protected chargerComponents: EdgeConfig.Component[] = [];
  protected productionMeterComponents: EdgeConfig.Component[] = [];
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
    this.chargerComponents =
      this.config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger")
        .filter(component => component.isEnabled);

    // Get productionMeters
    this.productionMeterComponents =
      this.config.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
        .filter(component => component.isEnabled && this.config.isProducer(component));

    const sum: EdgeConfig.Component = this.config.getComponent("_sum");
    sum.alias = this.translate.instant("General.TOTAL");

    this.navigationButtons = [sum, ...this.chargerComponents, ...this.productionMeterComponents].map(el => (
      { id: el.id, alias: el.alias, callback: () => { this.router.navigate(["./" + el.id], { relativeTo: this.route }); } }
    ));
    return [];
  }
}
