import { Component } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { CurrentData } from "src/app/shared/edge/currentdata";
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";

@Component({
    selector: 'abstractWidgetLine',
    templateUrl: './abstractWidgetLine.component.html'
})

export class AbstractWidgetLineComponent {

    public edge: Edge = null;
    public config: EdgeConfig = null;
    public currentData = CurrentData;
    public service: Service;
    public route: ActivatedRoute;
    constructor(
        public websocket: Websocket
    ) { }

    public subscribeOnChannels(selector: string, channelAddress: ChannelAddress[]) {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.service.getConfig().then(config => {
                this.config = config;
                this.edge = edge;

                /** subscribing on the passed selector and channelAddress */
                this.edge.subscribeChannels(this.websocket, selector, channelAddress);
            })
        });
    }
    public unsubscribe(selector: string) {
        this.edge.unsubscribeChannels(this.websocket, selector);
    }
}