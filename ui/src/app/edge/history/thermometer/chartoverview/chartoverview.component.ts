import { ActivatedRoute } from '@angular/router';
import { Component, OnInit } from '@angular/core';
import { Service, Utils, EdgeConfig, Edge } from '../../../../shared/shared';

@Component({
    selector: ThermometerChartOverviewComponent.SELECTOR,
    templateUrl: './chartoverview.component.html'
})
export class ThermometerChartOverviewComponent implements OnInit {

    private static readonly SELECTOR = "thermometer-chart-overview";

    public edge: Edge = null;

    public component: EdgeConfig.Component = null;

    constructor(
        public service: Service,
        private route: ActivatedRoute,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.service.getConfig().then(config => {
                this.edge = edge;
            })
        });
    }
}