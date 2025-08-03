import { Component, effect, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { UserService } from "src/app/shared/service/user.service";
import { Edge, EdgeConfig, Service } from "../../../../shared/shared";

@Component({
    selector: DelayedSellToGridChartOverviewComponent.SELECTOR,
    templateUrl: "./delayedselltogridchartoverview.component.html",
    standalone: false,
})
export class DelayedSellToGridChartOverviewComponent implements OnInit {

    private static readonly SELECTOR = "symmetricpeakshaving-chart-overview";
    public edge: Edge | null = null;
    public component: EdgeConfig.Component | null = null;

    /** @deprecated used for new navigation migration purposes */
    protected newNavigationUrlSegment: string = "";

    constructor(
        public service: Service,
        private route: ActivatedRoute,
        private userService: UserService,
    ) {
        effect(() => {
            const isNewNavigation = this.userService.isNewNavigation();
            this.newNavigationUrlSegment = isNewNavigation ? "/live" : "";
        });
    }

    ngOnInit() {
        this.service.getCurrentEdge().then(edge => {
            this.service.getConfig().then(config => {
                this.edge = edge;
                this.component = config.getComponent(this.route.snapshot.params.componentId);
            });
        });
    }
}
