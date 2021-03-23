import { ActivatedRoute } from '@angular/router';
import { Component } from '@angular/core';
import { Service, Utils, EdgeConfig, Edge } from '../../../../shared/shared';

@Component({
    selector: SinglethresholdChartOverviewComponent.SELECTOR,
    templateUrl: './singlethresholdchartoverview.component.html'
})
export class SinglethresholdChartOverviewComponent {

    private static readonly SELECTOR = "channelthreshold-chart-overview";

    public edge: Edge = null;

    public component: EdgeConfig.Component = null;
    public inputChannel: string;

    // reference to the Utils method to access via html
    public isLastElement = Utils.isLastElement;

    constructor(
        public service: Service,
        private route: ActivatedRoute,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.service.getConfig().then(config => {
                this.edge = edge;
                this.component = config.getComponent(this.route.snapshot.params.componentId);
                this.inputChannel = config.getComponentProperties(this.component.id)['inputChannelAddress'];
            })
        });
    }
}