import { Component } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { filter, takeUntil } from "rxjs/operators";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";
import { NavigationOption } from "src/app/shared/components/footer/subnavigation/footerNavigation";
import { EdgeConfig, Service } from "src/app/shared/shared";


@Component({
    templateUrl: "./OVERVIEW.HTML",
    standalone: false,
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

        THIS.SERVICE.HISTORY_PERIOD.PIPE(takeUntil(THIS.STOP_ON_DESTROY), filter(period => !!period))
            .subscribe((period) => {
                THIS.IS_ALLOWED = PERIOD.IS_WEEK_OR_DAY();
            });

        const navigationButtons: EDGE_CONFIG.COMPONENT[] = [];
        const gridMeters = OBJECT.VALUES(THIS.CONFIG.COMPONENTS)
            .filter((component) => COMPONENT.IS_ENABLED && THIS.CONFIG.IS_TYPE_GRID(component));

        if (!gridMeters) {
            return;
        }

        NAVIGATION_BUTTONS.PUSH(...gridMeters);

        THIS.NAVIGATION_BUTTONS = NAVIGATION_BUTTONS.MAP(el => (
            { id: EL.ID, alias: NAVIGATION_BUTTONS.LENGTH === 1 ? THIS.TRANSLATE.INSTANT("EDGE.HISTORY.PHASE_ACCURATE") : EL.ALIAS, callback: () => { THIS.ROUTER.NAVIGATE(["./" + EL.ID], { relativeTo: THIS.ROUTE }); } }
        ));
    }
}
