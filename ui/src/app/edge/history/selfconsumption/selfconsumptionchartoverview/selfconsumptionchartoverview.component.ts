import { Component } from '@angular/core';
import { Service, Edge } from '../../../../shared/shared';
import { ActivatedRoute } from '@angular/router';

@Component({
    selector: SelfconsumptionChartOverviewComponent.SELECTOR,
    templateUrl: './selfconsumptionchartoverview.component.html'
})
export class SelfconsumptionChartOverviewComponent {

    private static readonly SELECTOR = "selfconsumption-chart-overview";

    public edge: Edge = null;

    constructor(
        public service: Service,
        private route: ActivatedRoute,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
        });
    }
}