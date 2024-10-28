import { Component } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";
import { NavigationOption } from "src/app/shared/components/footer/subnavigation/footerNavigation";
import { EdgeConfig, Service } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";

@Component({
  templateUrl: "./details.overview.html",
})
export class DetailsOverviewComponent extends AbstractHistoryChartOverview {
  protected navigationButtons: NavigationOption[] = [];
  protected title: string | null = null;
  protected gridMeters: EdgeConfig.Component[] = [];

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
    this.service.getCurrentEdge().then(edge => {

      if (!this.component) {
        return;
      }

      const gridMeter = this.config.isTypeGrid(this.component) ?? null;
      if (!gridMeter) {
        return;
      }

      const gridMeters = Object.values(this.config.components)
        .filter((comp) => comp.isEnabled && this.config.isTypeGrid(comp)) ?? null;

      if (gridMeters?.length == 1) {
        this.title = this.translate.instant("General.grid");
      }

      this.navigationButtons = [
        { id: "currentVoltage", isEnabled: edge.roleIsAtLeast(Role.INSTALLER), alias: this.translate.instant("Edge.History.CURRENT_AND_VOLTAGE"), callback: () => { this.router.navigate(["./currentVoltage"], { relativeTo: this.route }); } }];
    });
  }
}
