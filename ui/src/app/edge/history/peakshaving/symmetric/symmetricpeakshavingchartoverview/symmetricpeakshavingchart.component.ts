import { Component } from '@angular/core';
import { Service, EdgeConfig, Edge } from '../../../../../shared/shared';
import { ActivatedRoute } from '@angular/router';

@Component({
    selector: SymmetricPeakshavingChartoOverviewComponent.SELECTOR,
    templateUrl: './symmetricpeakshavingchart.component.html'
})
export class SymmetricPeakshavingChartoOverviewComponent {

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