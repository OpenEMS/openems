import { Component } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { CurrentData } from "src/app/shared/edge/currentdata";
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";

@Component({
    selector: AbstractWidget.SELECTOR,
    templateUrl: 'abstractWidget.html'
})
export abstract class AbstractWidget {
    static SELECTOR: string = 'abstractWidget';
    public edge: Edge = null;
    public config: EdgeConfig = null;
    constructor(
        public service: Service,
        private websocket: Websocket,
        private route: ActivatedRoute,
    ) { }
    public subscribeChannels(selector: string, channelAddress: ChannelAddress[]) {
        this.service.getConfig().then(config => {
            this.config = config;
            this.service.setCurrentComponent('', this.route).then(edge => {
                this.edge = edge;
                console.log("abstractWidget gets here", channelAddress)
                this.edge.subscribeChannels(this.websocket, selector, channelAddress);
                console.log("AbstractWidget: ---------------", channelAddress)
            })
        });
    }
}
// this.service.setCurrentComponent('', this.route).then(edge => {
    // this.edge = edge;