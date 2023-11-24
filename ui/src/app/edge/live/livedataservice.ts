import { Directive, Inject, OnDestroy } from "@angular/core";
import { takeUntil } from "rxjs/operators";
import { v4 as uuidv4 } from 'uuid';

import { DataService } from "../../shared/genericComponents/shared/dataservice";
import { ChannelAddress, Edge, Service, Websocket } from "../../shared/shared";

@Directive()
export class LiveDataService extends DataService implements OnDestroy {

    private subscribeId: string | null = null;
    private subscribedChannelAddresses: ChannelAddress[] = [];

    constructor(
        @Inject(Websocket) protected websocket: Websocket,
        @Inject(Service) protected service: Service,
    ) {
        super();
    }

    public getValues(channelAddresses: ChannelAddress[], edge: Edge, componentId: string) {

        for (let channelAddress of channelAddresses) {
            this.subscribedChannelAddresses.push(channelAddress);
        }

        this.subscribeId = uuidv4();
        this.edge = edge;
        if (channelAddresses.length != 0) {
            edge.subscribeChannels(this.websocket, this.subscribeId, channelAddresses);
        }

        // call onCurrentData() with latest data
        edge.currentData.pipe(takeUntil(this.stopOnDestroy)).subscribe(currentData => {
            let allComponents = this.currentValue.value.allComponents;
            for (let channelAddress of channelAddresses) {
                let ca = channelAddress.toString();
                allComponents[ca] = currentData.channel[ca];
            }

            this.currentValue.next({ allComponents: allComponents });
        });
    }

    ngOnDestroy() {
        this.edge.unsubscribeFromChannels(this.websocket, this.subscribedChannelAddresses);
        this.stopOnDestroy.next();
        this.stopOnDestroy.complete();
    }

    public unsubscribeFromChannels(channels: ChannelAddress[]) {
        this.edge.unsubscribeFromChannels(this.websocket, channels);
    }
}
