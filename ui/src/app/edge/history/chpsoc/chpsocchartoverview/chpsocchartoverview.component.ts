import { Component, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { Edge, EdgeConfig, Service } from "../../../../shared/shared";

@Component({
    selector: ChpSocChartOverviewComponent.SELECTOR,
    templateUrl: "./chpsocchartoverview.component.html",
})
export class ChpSocChartOverviewComponent implements OnInit {

    private static readonly SELECTOR = "chpsoc-chart-overview";
    public edge: Edge | null = null;
    public config: EdgeConfig | null = null;
    public component: EdgeConfig.Component | null = null;

    constructor(
        public service: Service,
        private route: ActivatedRoute,
    ) { }

    ngOnInit() {
        this.service.getCurrentEdge().then(edge => {
            this.service.getConfig().then(config => {
                this.edge = edge;
                this.config = config;
                this.component = config.getComponent(this.route.snapshot.params.componentId);
            });
        });
    }
}
