import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Edge, Service } from '../../../../shared/shared';

@Component({
    selector: GridChartOverviewComponent.SELECTOR,
    templateUrl: './gridchartoverview.component.html'
})
export class GridChartOverviewComponent implements OnInit {

    private static readonly SELECTOR = "grid-chart-overview";

    public edge: Edge = null;

    public showPhases: boolean = false;

    constructor(
        private route: ActivatedRoute,
        public service: Service
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
        });
    }

    onNotifyPhases(showPhases: boolean): void {
        this.showPhases = showPhases;
    }
}