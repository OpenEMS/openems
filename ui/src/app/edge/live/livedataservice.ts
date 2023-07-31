import { Inject, Injectable, OnDestroy } from "@angular/core";
import { takeUntil } from "rxjs/operators";
import { v4 as uuidv4 } from 'uuid';
import { DataService } from "../../shared/genericComponents/shared/dataservice";
import { ChannelAddress, Edge, Service, Websocket } from "../../shared/shared";

@Injectable()
export class LiveDataService extends DataService implements OnDestroy {

    private subscribeId: string | null = null;

    constructor(
        @Inject(Websocket) protected websocket: Websocket,
        @Inject(Service) protected service: Service
    ) {
        super();
    }

    public getValues(channelAddresses: ChannelAddress[], edge: Edge, componentId: string) {
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
        this.stopOnDestroy.next();
        this.stopOnDestroy.complete();
    }
}
