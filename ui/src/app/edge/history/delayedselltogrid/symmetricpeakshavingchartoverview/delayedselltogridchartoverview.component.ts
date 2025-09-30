import { Component, effect, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { UserService } from "src/app/shared/service/USER.SERVICE";
import { Edge, EdgeConfig, Service } from "../../../../shared/shared";

@Component({
    selector: DELAYED_SELL_TO_GRID_CHART_OVERVIEW_COMPONENT.SELECTOR,
    templateUrl: "./DELAYEDSELLTOGRIDCHARTOVERVIEW.COMPONENT.HTML",
    standalone: false,
})
export class DelayedSellToGridChartOverviewComponent implements OnInit {

    private static readonly SELECTOR = "symmetricpeakshaving-chart-overview";
    public edge: Edge | null = null;
    public component: EDGE_CONFIG.COMPONENT | null = null;

    /** @deprecated used for new navigation migration purposes */
    protected newNavigationUrlSegment: string = "";

    constructor(
        public service: Service,
        private route: ActivatedRoute,
        private userService: UserService,
    ) {
        effect(() => {
            const isNewNavigation = THIS.USER_SERVICE.IS_NEW_NAVIGATION();
            THIS.NEW_NAVIGATION_URL_SEGMENT = isNewNavigation ? "/live" : "";
        });
    }

    ngOnInit() {
        THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {
            THIS.SERVICE.GET_CONFIG().then(config => {
                THIS.EDGE = edge;
                THIS.COMPONENT = CONFIG.GET_COMPONENT(THIS.ROUTE.SNAPSHOT.PARAMS.COMPONENT_ID);
            });
        });
    }
}
