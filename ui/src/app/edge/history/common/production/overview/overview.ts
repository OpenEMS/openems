import { Component, inject } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { NavigationOption } from "src/app/shared/components/footer/subnavigation/footerNavigation";
import { AbstractHistoryChartOverview } from "../../../../../shared/components/chart/abstractHistoryChartOverview";
import { ChannelAddress, EdgeConfig, Service } from "../../../../../shared/shared";

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

  protected chargerComponents: EdgeConfig.Component[] = [];
  protected productionMeterComponents: EdgeConfig.Component[] = [];
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

  protected override getChannelAddresses(): ChannelAddress[] {
    //  Get Chargers
    this.chargerComponents =
      this.config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger")
        .filter(component => component.isEnabled);

    // Get productionMeters
    this.productionMeterComponents =
      this.config.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
        .filter(component => component.isEnabled && this.config.isProducer(component));


    this.navigationButtons = [...this.productionMeterComponents, ...this.chargerComponents].map(el => (
      { id: el.id, alias: el.alias, callback: () => { this.router.navigate(["./" + el.id], { relativeTo: this.route }); } }
    ));
    return [];
  }
}
