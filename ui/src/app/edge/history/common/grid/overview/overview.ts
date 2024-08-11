import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { AbstractHistoryChartOverview } from 'src/app/shared/components/chart/abstractHistoryChartOverview';
import { NavigationOption } from 'src/app/shared/components/footer/subnavigation/footerNavigation';
import { EdgeConfig, Service } from 'src/app/shared/shared';

@Component({
    templateUrl: './overview.html',
})
export class OverviewComponent extends AbstractHistoryChartOverview {
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

        const sum: EdgeConfig.Component = this.config.getComponent('_sum');
        sum.alias = this.translate.instant('General.TOTAL');
        const navigationButtons: EdgeConfig.Component[] = [];
        const gridMeters = Object.values(this.config.components)
            .filter((component) => component.isEnabled && this.config.isTypeGrid(component));

        if (!gridMeters) {
            navigationButtons.push(sum);
        }

        if (gridMeters?.length <= 1) {
            navigationButtons.push(sum);
        } else {
            navigationButtons.push(...gridMeters);
        }

        this.navigationButtons = navigationButtons.map(el => (
            { id: el.id, alias: el.alias, callback: () => { this.router.navigate(['./' + el.id], { relativeTo: this.route }); } }
        ));
    }
}
