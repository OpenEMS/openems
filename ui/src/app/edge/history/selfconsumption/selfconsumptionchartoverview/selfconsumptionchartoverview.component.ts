import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Edge, Service } from '../../../../shared/shared';

@Component({
    selector: SelfconsumptionChartOverviewComponent.SELECTOR,
    templateUrl: './selfconsumptionchartoverview.component.html'
})
export class SelfconsumptionChartOverviewComponent implements OnInit {

    private static readonly SELECTOR = "selfconsumption-chart-overview";

    public edge: Edge = null;

    constructor(
        public service: Service,
        private route: ActivatedRoute
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
        });
    }
}