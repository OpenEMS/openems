import { ActivatedRoute } from "@angular/router";
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";

export abstract class AbstractFlatWidgetComponent {

    public edge: Edge = null;
    public config: EdgeConfig = null;
    public service: Service = null;
    public route: ActivatedRoute = null;

    constructor(
        public websocket: Websocket
    ) { }

    protected subscribeOnChannels(selector: string, channelAddress: ChannelAddress[]) {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;

            /** subscribing on the passed selector and channelAddress */
            this.edge.subscribeChannels(this.websocket, selector, channelAddress);
        });
    }

    protected unsubscribe(selector: string) {
        this.edge.unsubscribeChannels(this.websocket, selector);
    }
}