import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Edge, EdgeConfig, Service } from '../../../../shared/shared';

@Component({
    selector: ChpSocChartOverviewComponent.SELECTOR,
    templateUrl: './chpsocchartoverview.component.html',
})
export class ChpSocChartOverviewComponent implements OnInit {

    public edge: Edge = null;
    public config: EdgeConfig = null;

    public component: EdgeConfig.Component = null;

    private static readonly SELECTOR = "chpsoc-chart-overview";

    constructor(
        public service: Service,
        private route: ActivatedRoute,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.service.getConfig().then(config => {
                this.edge = edge;
                this.config = config;
                this.component = config.getComponent(this.route.snapshot.params.componentId);
            });
        });
    }
}
