import { Component } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";
import { NavigationOption } from "src/app/shared/components/footer/subnavigation/footerNavigation";
import { EdgeConfig, Service } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";

@Component({
  templateUrl: "./DETAILS.OVERVIEW.HTML",
  standalone: false,
})
export class DetailsOverviewComponent extends AbstractHistoryChartOverview {
  protected navigationButtons: NavigationOption[] = [];
  protected title: string | null = null;
  protected gridMeters: EDGE_CONFIG.COMPONENT[] = [];

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
    THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {

      if (!THIS.COMPONENT) {
        return;
      }

      const gridMeter = THIS.CONFIG.IS_TYPE_GRID(THIS.COMPONENT) ?? null;
      if (!gridMeter) {
        return;
      }

      const gridMeters = OBJECT.VALUES(THIS.CONFIG.COMPONENTS)
        .filter((comp) => COMP.IS_ENABLED && THIS.CONFIG.IS_TYPE_GRID(comp)) ?? null;

      if (gridMeters?.length == 1) {
        THIS.TITLE = THIS.TRANSLATE.INSTANT("GENERAL.GRID");
      }

      THIS.NAVIGATION_BUTTONS = [
        { id: "currentVoltage", isEnabled: EDGE.ROLE_IS_AT_LEAST(ROLE.INSTALLER), alias: THIS.TRANSLATE.INSTANT("EDGE.HISTORY.CURRENT_AND_VOLTAGE"), callback: () => { THIS.ROUTER.NAVIGATE(["./currentVoltage"], { relativeTo: THIS.ROUTE }); } }];
    });
  }
}
