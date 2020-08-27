import { Component, Input } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Service, Utils, EdgeConfig, Edge } from '../../../../shared/shared';
import { ActivatedRoute } from '@angular/router';

@Component({
    selector: ChannelthresholdChartOverviewComponent.SELECTOR,
    templateUrl: './channelthresholdchartoverview.component.html'
})
export class ChannelthresholdChartOverviewComponent {


    private static readonly SELECTOR = "channelthreshold-chart-overview";

    public edge: Edge = null;
    public config: EdgeConfig;

    public component: EdgeConfig.Component;

    public showTotal: boolean = null;
    public channelthresholdComponents: string[] = [];

    // referene to the Utils method to access via html
    public isLastElement = Utils.isLastElement;

    constructor(
        public service: Service,
        private route: ActivatedRoute,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
        });

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