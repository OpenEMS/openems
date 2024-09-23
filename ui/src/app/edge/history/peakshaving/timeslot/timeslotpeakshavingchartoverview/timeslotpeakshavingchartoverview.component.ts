import { Component, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { Edge, EdgeConfig, Service } from "../../../../../shared/shared";

@Component({
    selector: TimeslotPeakshavingChartOverviewComponent.SELECTOR,
    templateUrl: "./timeslotpeakshavingchartoverview.component.html",
})
export class TimeslotPeakshavingChartOverviewComponent implements OnInit {

    private static readonly SELECTOR = "timeslotpeakshaving-chart-overview";

    public edge: Edge | null = null;
    public component: EdgeConfig.Component | null = null;

    constructor(
        public service: Service,
        private route: ActivatedRoute,
    ) { }

    ngOnInit() {
        this.service.getCurrentEdge().then(edge => {
            this.service.getConfig().then(config => {
                this.edge = edge;
                this.component = config.getComponent(this.route.snapshot.params.componentId);
            });
        });
    }
}
