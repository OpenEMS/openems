import { ActivatedRoute } from '@angular/router';
import { Component } from '@angular/core';
import { Service, Edge } from '../../../../shared/shared';

@Component({
    selector: GridChartOverviewComponent.SELECTOR,
    templateUrl: './gridchartoverview.component.html'
})
export class GridChartOverviewComponent {

    private static readonly SELECTOR = "grid-chart-overview";

    public edge: Edge = null;

    public showPhases: boolean = false;

    constructor(
        private route: ActivatedRoute,
        public service: Service,
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