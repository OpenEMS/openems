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
    THIS.COMPONENT_SOME = THIS.GET_COMPONENT_TYPE();

    THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {

      // Hide current & voltage
      if (THIS.COMPONENT?.factoryId === "CORE.SUM") {
        return;
      }

      THIS.NAVIGATION_BUTTONS = [
        { id: "currentVoltage", isEnabled: EDGE.ROLE_IS_AT_LEAST(ROLE.INSTALLER), alias: THIS.TRANSLATE.INSTANT("EDGE.HISTORY.CURRENT_AND_VOLTAGE"), callback: () => { THIS.ROUTER.NAVIGATE(["./currentVoltage"], { relativeTo: THIS.ROUTE }); } }];
    });
  }

  private getComponentType(): typeof THIS.COMPONENT_SOME {
    if (!THIS.COMPONENT) {
      return null;
    }

    if (THIS.CONFIG.HAS_COMPONENT_NATURE("IO.OPENEMS.EDGE.ESS.DCCHARGER.API.ESS_DC_CHARGER", THIS.COMPONENT.ID) && THIS.COMPONENT.IS_ENABLED) {
      return { type: "charger", displayName: THIS.COMPONENT.ALIAS };
    }

    if (THIS.CONFIG.IS_PRODUCER(THIS.COMPONENT) && THIS.COMPONENT.IS_ENABLED) {
      return { type: "productionMeter", displayName: THIS.COMPONENT.ALIAS };
    }

    if (THIS.COMPONENT.FACTORY_ID === "CORE.SUM") {
      return { type: "sum", displayName: THIS.TRANSLATE.INSTANT("GENERAL.TOTAL") };
    }

    return null;
  }
}
