import { ActivatedRoute } from '@angular/router';
import { Component } from '@angular/core';
import { Service, Utils, EdgeConfig, Edge } from '../../../../shared/shared';

@Component({
    selector: ChannelthresholdChartOverviewComponent.SELECTOR,
    templateUrl: './channelthresholdchartoverview.component.html'
})
export class ChannelthresholdChartOverviewComponent {

    private static readonly SELECTOR = "channelthreshold-chart-overview";

    public edge: Edge = null;
    public config: EdgeConfig = null;

    public component: EdgeConfig.Component = null;

    public showTotal: boolean = null;
    public channelthresholdComponents: string[] = [];

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
                this.config = config;
                this.component = config.getComponent(this.route.snapshot.params.componentId);
                for (let componentId of
                    config.getComponentIdsImplementingNature("io.openems.impl.controller.channelthreshold.ChannelThresholdController")
                        .concat(this.config.getComponentIdsByFactory("Controller.ChannelThreshold"))) {
                    this.channelthresholdComponents.push(componentId)
                }
                if (this.channelthresholdComponents.length > 1) {
                    this.showTotal = false;
                } else if (this.channelthresholdComponents.length == 1) {
                    this.showTotal = null;
                }
            })
        });
    }

    onNotifyTotal(showTotal: boolean): void {
        this.showTotal = showTotal;
    }
}