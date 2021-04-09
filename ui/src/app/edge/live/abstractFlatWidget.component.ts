import { Inject, Injectable, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { UUID } from "angular2-uuid";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { CurrentData } from "src/app/shared/edge/currentdata";
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";

@Injectable()
export abstract class AbstractFlatWidgetComponent implements OnInit {

    public edge: Edge = null;
    public config: EdgeConfig = null;
    public component: EdgeConfig.Component = null;

    public service: Service = null;
    public route: ActivatedRoute = null;
    private stopOnDestroy: Subject<void> = new Subject<void>();
    public randomselector: string = UUID.UUID().toString();
    public outputChannel: string;
    protected componentId: string;


    @Inject(Websocket) private websocket: Websocket;

    public ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
            console.log("1")
            this.service.getConfig().then(config => {
                console.log("2")
                this.getChannelAddressess(edge, config);
                console.log("3")
                this.getComponentId(this.componentId);
                console.log(4)
                this.component = this.config.components[this.componentId];
                console.log(5)
                console.log("componentId", this.componentId)
                console.log(6)
                this.outputChannel = this.component.properties['outputChannelAddress'];
                console.log(7)
                // if (this.channel !== undefined && this.channel != null) {
                //     if (this.channel.includes('/')) {
                //         /** Calls method of parentClass AbstractWidgetLine for Subscribing on channels*/
                //         this.subscribeOnChannels(this.randomSelector, [ChannelAddress.fromString(this.channel)]);
                //     }
                // }
                this.subscribeOnChannels;
                this.edge.currentData.pipe(takeUntil(this.stopOnDestroy)).subscribe(currentData => {
                    currentData.channel[this.outputChannel];
                })
                this.edge.subscribeChannels(this.websocket, 'something', [ChannelAddress.fromString(this.outputChannel)])
            })
        });

        // currentData subscribe
    };
    protected onCurrentData(currentData: CurrentData) { }
    protected getComponentId(componentId: string): string {
        return componentId = this.componentId;
    }
    // unsubscribe

    ngOnDestroy() {
        this.stopOnDestroy.next();
        this.stopOnDestroy.complete();
    }

    protected abstract getChannelAddressess(edge: Edge, config: EdgeConfig);

    protected subscribeOnChannels(selector: string, channelAddress: ChannelAddress[]) {
        /** subscribing on the passed selector and channelAddress */
        // if (this.channel !== undefined && this.channel != null) {
        //     if (this.channel.includes('/')) {
        console.log("channeladdress", channelAddress)
        this.edge.subscribeChannels(this.websocket, selector, channelAddress);
    }

    protected unsubscribe(selector: string) {
        this.edge.unsubscribeChannels(this.websocket, selector);
    }
}