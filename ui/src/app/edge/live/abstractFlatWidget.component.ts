import { Inject, Injectable, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";

@Injectable()
export abstract class AbstractFlatWidgetComponent implements OnInit {

    public edge: Edge = null;
    public config: EdgeConfig = null;
    public service: Service = null;
    public route: ActivatedRoute = null;

    @Inject(Websocket) private websocket: Websocket;

    public ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
            this.service.getConfig().then(config => {
                this.getChannelAddressess(edge, config);
                // TODO subscribe


            })
        });
        this.edge.currentData.pipe(takeUntil(this.stopOnDestroy)).subscribe(currentData => {
            this.onCurrentData(currentData);
        }
        // currentData subscribe
    }

    // unsubscribe

    ngOnDestroy() {
        this.stopOnDestroy.next();
        this.stopOnDestroy.complete();
    }

    protected abstract getChannelAddressess(edge: Edge, config: EdgeConfig);

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