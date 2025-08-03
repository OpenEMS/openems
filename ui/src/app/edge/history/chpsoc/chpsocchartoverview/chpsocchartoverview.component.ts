import { Component, OnInit, inject } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { Edge, EdgeConfig, Service } from "../../../../shared/shared";

@Component({
    selector: ChpSocChartOverviewComponent.SELECTOR,
    templateUrl: "./chpsocchartoverview.component.html",
})
export class ChpSocChartOverviewComponent implements OnInit {
    service = inject(Service);
    private route = inject(ActivatedRoute);


    private static readonly SELECTOR = "chpsoc-chart-overview";
    public edge: Edge | null = null;
    public config: EdgeConfig | null = null;
    public component: EdgeConfig.Component | null = null;

    /** Inserted by Angular inject() migration for backwards compatibility */
    constructor(...args: unknown[]);

    constructor() { }

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
