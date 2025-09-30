import { Component, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { Edge, EdgeConfig, Service } from "../../../../shared/shared";

@Component({
    selector: CHP_SOC_CHART_OVERVIEW_COMPONENT.SELECTOR,
    templateUrl: "./CHPSOCCHARTOVERVIEW.COMPONENT.HTML",
})
export class ChpSocChartOverviewComponent implements OnInit {

    private static readonly SELECTOR = "chpsoc-chart-overview";
    public edge: Edge | null = null;
    public config: EdgeConfig | null = null;
    public component: EDGE_CONFIG.COMPONENT | null = null;

    constructor(
        public service: Service,
        private route: ActivatedRoute,
    ) { }

    ngOnInit() {
        THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {
            THIS.SERVICE.GET_CONFIG().then(config => {
                THIS.EDGE = edge;
                THIS.CONFIG = config;
                THIS.COMPONENT = CONFIG.GET_COMPONENT(THIS.ROUTE.SNAPSHOT.PARAMS.COMPONENT_ID);
            });
        });
    }
}
