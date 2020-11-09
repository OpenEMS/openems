import { ActivatedRoute } from '@angular/router';
import { Component } from '@angular/core';
import { Service, EdgeConfig, Edge } from '../../../../../shared/shared';

@Component({
    selector: SymmetricPeakshavingChartOverviewComponent.SELECTOR,
    templateUrl: './symmetricpeakshavingchartoverview.component.html'
})
export class SymmetricPeakshavingChartOverviewComponent {

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