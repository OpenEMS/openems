import { Component } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";
import { NavigationOption } from "src/app/shared/components/footer/subnavigation/footerNavigation";
import { Service } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";

@Component({
  templateUrl: "./DETAILS.OVERVIEW.HTML",
  standalone: false,
})
export class DetailsOverviewComponent extends AbstractHistoryChartOverview {
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
    THIS.COMPONENT_TYPE = THIS.GET_COMPONENT_TYPE();
    THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {

      if (!THIS.COMPONENT) {
        return;
      }

      if (THIS.CONFIG?.hasComponentNature("IO.OPENEMS.EDGE.EVCS.API.EVCS", THIS.COMPONENT.ID)) {
        return;
      }

      if (THIS.COMPONENT.FACTORY_ID === "CORE.SUM") {
        THIS.COMPONENT.ALIAS = THIS.TRANSLATE.INSTANT("GENERAL.TOTAL");
        return;
      }

      if (THIS.COMPONENT.FACTORY_ID === "HEAT.ASKOMA") {
        return;
      }

      THIS.NAVIGATION_BUTTONS = [
        { id: "currentVoltage", isEnabled: EDGE.ROLE_IS_AT_LEAST(ROLE.INSTALLER), alias: THIS.TRANSLATE.INSTANT("EDGE.HISTORY.CURRENT_AND_VOLTAGE"), callback: () => { THIS.ROUTER.NAVIGATE(["./currentVoltage"], { relativeTo: THIS.ROUTE }); } }];
    });
  }

  private getComponentType(): typeof THIS.COMPONENT_TYPE {
    if (!THIS.COMPONENT) {
      return null;
    }

    if (THIS.CONFIG?.hasComponentNature("IO.OPENEMS.EDGE.EVCS.API.EVCS", THIS.COMPONENT.ID)
      && (THIS.COMPONENT.FACTORY_ID !== "EVCS.CLUSTER.SELF_CONSUMPTION")
      && THIS.COMPONENT.FACTORY_ID !== "EVCS.CLUSTER.PEAK_SHAVING"
      && THIS.COMPONENT.IS_ENABLED !== false) {
      return "evcs";
    }

    if (THIS.CONFIG?.hasComponentNature("IO.OPENEMS.EDGE.HEAT.API.HEAT", THIS.COMPONENT.ID)
      && (THIS.COMPONENT.FACTORY_ID !== "CONTROLLER.HEAT.HEATINGELEMENT")
      && THIS.COMPONENT.IS_ENABLED !== false) {
      return "heat";
    }

    if (THIS.CONFIG?.hasComponentNature("IO.OPENEMS.EDGE.METER.API.ELECTRICITY_METER", THIS.COMPONENT.ID)
      && THIS.CONFIG.IS_TYPE_CONSUMPTION_METERED(THIS.COMPONENT) && THIS.COMPONENT.IS_ENABLED) {
      return "consumptionMeter";
    }

    if (THIS.COMPONENT.FACTORY_ID === "CORE.SUM") {
      return "sum";
    }

    return null;
  }
}
