import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Edge, EdgeConfig, Service, Utils } from '../../../../shared/shared';

@Component({
    selector: ChannelthresholdChartOverviewComponent.SELECTOR,
    templateUrl: './channelthresholdchartoverview.component.html'
})
export class ChannelthresholdChartOverviewComponent implements OnInit {

    private static readonly SELECTOR = "channelthreshold-chart-overview";

    public edge: Edge = null;
    public config: EdgeConfig = null;

    public component: EdgeConfig.Component = null;

    public showTotal: boolean = false;
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
                    this.channelthresholdComponents.push(componentId);
                }
                if (this.channelthresholdComponents.length > 0) {
                    this.showTotal = false;
                }
            });
        });
    }

    onNotifyTotal(showTotal: boolean): void {
        this.showTotal = showTotal;
    }
}