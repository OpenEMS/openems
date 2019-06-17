import { Component, Input, Output } from '@angular/core';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from '../../../shared/shared';
import { ActivatedRoute } from '@angular/router';

@Component({
    selector: ChpsocComponent.SELECTOR,
    templateUrl: './chpsoc.component.html'
})
export class ChpsocComponent {

    private static readonly SELECTOR = "chpSoc";

    @Input() private componentId: string;

    public edge: Edge = null;
    public controller: EdgeConfig.Component = null;
    public inputChannel: ChannelAddress = null;
    public outputChannel: ChannelAddress = null;
    public lowThreshold: number;
    public highThresold: number;

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
                this.lowThreshold = (config.getComponentProperties(this.componentId)['lowThreshold']);
                this.highThresold = (config.getComponentProperties(this.componentId)['highThreshold']);
                edge.subscribeChannels(this.websocket, ChpsocComponent.SELECTOR + this.componentId, [
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