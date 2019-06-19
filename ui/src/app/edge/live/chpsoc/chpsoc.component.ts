import { Component, Input, Output } from '@angular/core';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from '../../../shared/shared';
import { ActivatedRoute } from '@angular/router';

@Component({
    selector: ChpSocComponent.SELECTOR,
    templateUrl: './chpsoc.component.html'
})
export class ChpSocComponent {

    private static readonly SELECTOR = "chpsoc";

    @Input() private componentId: string;

    private edge: Edge = null;
    private inputChannel: ChannelAddress = null;
    private outputChannel: ChannelAddress = null;
    private lowThreshold: number;
    private highThresold: number;

    constructor(
        private service: Service,
        private websocket: Websocket,
        private route: ActivatedRoute
    ) { }

    ngOnInit() {
        // Subscribe to CurrentData
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
            this.service.getConfig().then(config => {
                this.outputChannel = ChannelAddress.fromString(config.getComponentProperties(this.componentId)['outputChannelAddress']);
                this.inputChannel = ChannelAddress.fromString(config.getComponentProperties(this.componentId)['inputChannelAddress']);
                this.lowThreshold = config.getComponentProperties(this.componentId)['lowThreshold'];
                this.highThresold = config.getComponentProperties(this.componentId)['highThreshold'];
                edge.subscribeChannels(this.websocket, ChpSocComponent.SELECTOR + this.componentId, [
                    this.outputChannel,
                    this.inputChannel
                ]);
            });
        });
    }

    ngOnDestroy() {
        if (this.edge != null) {
            this.edge.unsubscribeChannels(this.websocket, ChpsocComponent.SELECTOR + this.componentId);
        }
    }
}