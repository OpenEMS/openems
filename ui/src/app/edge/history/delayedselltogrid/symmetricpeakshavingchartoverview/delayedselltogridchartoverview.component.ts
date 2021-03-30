import { ActivatedRoute } from '@angular/router';
import { Component } from '@angular/core';
import { Service, EdgeConfig, Edge } from '../../../../shared/shared';

@Component({
    selector: DelayedSellToGridChartOverviewComponent.SELECTOR,
    templateUrl: './delayedselltogridchartoverview.component.html'
})
export class DelayedSellToGridChartOverviewComponent {

    public edge: Edge = null;
    public component: EdgeConfig.Component = null;

    private static readonly SELECTOR = "symmetricpeakshaving-chart-overview";

    constructor(
        public service: Service,
        private route: ActivatedRoute,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.service.getConfig().then(config => {
                this.edge = edge;
                this.component = config.getComponent(this.route.snapshot.params.componentId);
            })
        })
    }
}