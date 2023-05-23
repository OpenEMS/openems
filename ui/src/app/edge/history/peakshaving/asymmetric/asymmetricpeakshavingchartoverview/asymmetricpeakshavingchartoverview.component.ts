import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Edge, EdgeConfig, Service } from '../../../../../shared/shared';

@Component({
    selector: AsymmetricPeakshavingChartOverviewComponent.SELECTOR,
    templateUrl: './asymmetricpeakshavingchartoverview.component.html'
})
export class AsymmetricPeakshavingChartOverviewComponent implements OnInit {

    public edge: Edge = null;
    public component: EdgeConfig.Component = null;

    private static readonly SELECTOR = "asymmetricpeakshaving-chart-overview";

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