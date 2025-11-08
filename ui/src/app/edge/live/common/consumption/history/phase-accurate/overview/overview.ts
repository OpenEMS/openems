import { Component } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";
import { NavigationOption } from "src/app/shared/components/footer/subnavigation/footerNavigation";
import { Service } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";

@Component({
  templateUrl: "./overview.html",
  standalone: false,
})
export class CommonConsumptionDetailsOverviewComponent extends AbstractHistoryChartOverview {
  protected navigationButtons: NavigationOption[] = [];
  protected componentType: "sum" | "consumptionMeter" | "evcs" | "heat" | null = null;

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
    this.componentType = this.getComponentType();
    this.service.getCurrentEdge().then(edge => {

      if (!this.component) {
        return;
      }

      if (this.config?.hasComponentNature("io.openems.edge.evcs.api.Evcs", this.component.id)) {
        return;
      }

      if (this.component.factoryId === "Core.Sum") {
        this.component.alias = this.translate.instant("GENERAL.TOTAL");
        return;
      }

      if (this.component.factoryId === "Heat.Askoma") {
        return;
      }

      this.navigationButtons = [
        { id: "currentVoltage", isEnabled: edge.roleIsAtLeast(Role.INSTALLER), alias: this.translate.instant("EDGE.HISTORY.CURRENT_AND_VOLTAGE"), callback: () => { this.router.navigate(["./currentVoltage"], { relativeTo: this.route }); } }];
    });
  }

  private getComponentType(): typeof this.componentType {
    if (!this.component) {
      return null;
    }

    if (this.config?.hasComponentNature("io.openems.edge.evcs.api.Evcs", this.component.id)
      && (this.component.factoryId !== "Evcs.Cluster.SelfConsumption")
      && this.component.factoryId !== "Evcs.Cluster.PeakShaving"
      && this.component.isEnabled !== false) {
      return "evcs";
    }

    if (this.config?.hasComponentNature("io.openems.edge.heat.api.Heat", this.component.id)
      && (this.component.factoryId !== "Controller.Heat.Heatingelement")
      && this.component.isEnabled !== false) {
      return "heat";
    }

    if (this.config?.hasComponentNature("io.openems.edge.meter.api.ElectricityMeter", this.component.id)
      && this.config.isTypeConsumptionMetered(this.component) && this.component.isEnabled) {
      return "consumptionMeter";
    }

    if (this.component.factoryId === "Core.Sum") {
      return "sum";
    }

    return null;
  }
}
