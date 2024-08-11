import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { AbstractHistoryChartOverview } from 'src/app/shared/components/chart/abstractHistoryChartOverview';
import { NavigationOption } from 'src/app/shared/components/footer/subnavigation/footerNavigation';
import { Service } from 'src/app/shared/shared';
import { Role } from 'src/app/shared/type/role';

@Component({
  templateUrl: './details.overview.html',
})
export class DetailsOverviewComponent extends AbstractHistoryChartOverview {
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

  protected override afterIsInitialized() {
    this.service.getCurrentEdge().then(edge => {

      const gridMeter = Object.values(this.config.components)
        .find((component) => component.isEnabled && this.config.isTypeGrid(component)) ?? null;

      if (!gridMeter) {
        return;
      }

      this.navigationButtons = [
        { id: 'currentVoltage', isEnabled: edge.roleIsAtLeast(Role.INSTALLER), alias: this.translate.instant("Edge.History.CURRENT_AND_VOLTAGE"), callback: () => { this.router.navigate([`../${gridMeter.id}/currentVoltage`], { relativeTo: this.route }); } }];
    });
  }
}
