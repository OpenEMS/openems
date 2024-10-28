import { Component } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";
import { NavigationOption } from "src/app/shared/components/footer/subnavigation/footerNavigation";
import { Service } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";

@Component({
  templateUrl: "./details.overview.html",
})
export class DetailsOverviewComponent extends AbstractHistoryChartOverview {
  protected navigationButtons: NavigationOption[] = [];

  protected componentSome: { type: "sum" | "productionMeter" | "charger", displayName: string } | null = null;

  constructor(
    public override service: Service,
    protected override route: ActivatedRoute,
    public override modalCtrl: ModalController,
    private router: Router,
    private translate: TranslateService,
  ) {
    super(service, route, modalCtrl);
  }

  protected override afterIsInitialized() {
    this.componentSome = this.getComponentType();

    this.service.getCurrentEdge().then(edge => {

      // Hide current & voltage
      if (this.component?.factoryId === "Core.Sum") {
        return;
      }

      this.navigationButtons = [
        { id: "currentVoltage", isEnabled: edge.roleIsAtLeast(Role.INSTALLER), alias: this.translate.instant("Edge.History.CURRENT_AND_VOLTAGE"), callback: () => { this.router.navigate(["./currentVoltage"], { relativeTo: this.route }); } }];
    });
  }

  private getComponentType(): typeof this.componentSome {
    if (!this.component) {
      return null;
    }

    if (this.config.hasComponentNature("io.openems.edge.ess.dccharger.api.EssDcCharger", this.component.id) && this.component.isEnabled) {
      return { type: "charger", displayName: this.component.alias };
    }

    if (this.config.isProducer(this.component) && this.component.isEnabled) {
      return { type: "productionMeter", displayName: this.component.alias };
    }

    if (this.component.factoryId === "Core.Sum") {
      return { type: "sum", displayName: this.translate.instant("General.TOTAL") };
    }

    return null;
  }
}
