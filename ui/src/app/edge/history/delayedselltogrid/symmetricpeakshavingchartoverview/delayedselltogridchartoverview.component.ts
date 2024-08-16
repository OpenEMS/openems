import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Edge, EdgeConfig, Service } from '../../../../shared/shared';

@Component({
    selector: DelayedSellToGridChartOverviewComponent.SELECTOR,
    templateUrl: './delayedselltogridchartoverview.component.html',
})
export class DelayedSellToGridChartOverviewComponent implements OnInit {

    private static readonly SELECTOR = "symmetricpeakshaving-chart-overview";
    public edge: Edge | null = null;
    public component: EdgeConfig.Component | null = null;

    constructor(
        public service: Service,
        private route: ActivatedRoute,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.service.getConfig().then(config => {
                this.edge = edge;
                this.component = config.getComponent(this.route.snapshot.params.componentId);
            });
        });
    }
}
