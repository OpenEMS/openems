import { Component } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { filter, takeUntil } from "rxjs/operators";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";
import { NavigationOption } from "src/app/shared/components/footer/subnavigation/footerNavigation";
import { EdgeConfig, Service } from "src/app/shared/shared";


@Component({
    templateUrl: "./overview.html",
})
export class OverviewComponent extends AbstractHistoryChartOverview {
    protected navigationButtons: NavigationOption[] = [];
    protected isAllowed: boolean = false;

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

        this.service.historyPeriod.pipe(takeUntil(this.stopOnDestroy), filter(period => !!period))
            .subscribe((period) => {
                this.isAllowed = period.isWeekOrDay();
            });

        const navigationButtons: EdgeConfig.Component[] = [];
        const gridMeters = Object.values(this.config.components)
            .filter((component) => component.isEnabled && this.config.isTypeGrid(component));

        if (!gridMeters) {
            return;
        }

        navigationButtons.push(...gridMeters);

        this.navigationButtons = navigationButtons.map(el => (
            { id: el.id, alias: navigationButtons.length === 1 ? this.translate.instant("Edge.History.PHASE_ACCURATE") : el.alias, callback: () => { this.router.navigate(["./" + el.id], { relativeTo: this.route }); } }
        ));
    }
}
