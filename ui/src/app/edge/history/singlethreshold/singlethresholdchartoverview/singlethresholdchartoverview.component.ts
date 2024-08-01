// @ts-strict-ignore
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, EdgeConfig, Service, Utils, Websocket } from '../../../../shared/shared';

@Component({
    selector: SinglethresholdChartOverviewComponent.SELECTOR,
    templateUrl: './singlethresholdchartoverview.component.html',
})
export class SinglethresholdChartOverviewComponent implements OnInit {

    private static readonly SELECTOR = "channelthreshold-chart-overview";

    public edge: Edge | null = null;

    public component: EdgeConfig.Component | null = null;
    public inputChannel: string;

    // reference to the Utils method to access via html
    public isLastElement = Utils.isLastElement;

    protected inputChannelUnit: string;
    protected readonly spinnerid = SinglethresholdChartOverviewComponent.SELECTOR;


    constructor(
        public service: Service,
        private route: ActivatedRoute,
        private websocket: Websocket,
    ) { }

    ngOnInit() {
        this.service.startSpinner(this.spinnerid);
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.service.getConfig().then(config => {
                this.edge = edge;
                this.component = config.getComponent(this.route.snapshot.params.componentId);
                this.inputChannel = config.getComponentProperties(this.component.id)['inputChannelAddress'];

                this.edge.getChannel(this.websocket, ChannelAddress.fromString(this.inputChannel)).then(c => {
                    this.inputChannelUnit = c.unit;
                }).catch(e => {
                    console.error(e);
                    this.inputChannelUnit = '';
                }).finally(() => {
                    this.service.stopSpinner(this.spinnerid);
                });
            });
        });
    }
}
