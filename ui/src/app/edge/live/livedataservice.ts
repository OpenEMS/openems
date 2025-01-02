// @ts-strict-ignore
import { Directive, Inject, OnDestroy } from "@angular/core";
import { RefresherCustomEvent } from "@ionic/angular";
import { takeUntil } from "rxjs/operators";
import { v4 as uuidv4 } from "uuid";
import { AppService } from "src/app/app.service";
import { DataService } from "../../shared/components/shared/dataservice";
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

        this.service.getCurrentEdge().then((edge) => {
            edge.currentData.pipe(takeUntil(this.stopOnDestroy))
                .subscribe(() => this.lastUpdated.set(new Date()));
        });
    }

    public getValues(channelAddresses: ChannelAddress[], edge: Edge, componentId: string) {

        for (const channelAddress of channelAddresses) {
            this.subscribedChannelAddresses.push(channelAddress);
        }

        this.subscribeId = uuidv4();
        this.edge = edge;
        if (channelAddresses.length != 0) {
            edge.subscribeChannels(this.websocket, this.subscribeId, channelAddresses);
        }

        // call onCurrentData() with latest data
        edge.currentData.pipe(takeUntil(this.stopOnDestroy)).subscribe(currentData => {
            const allComponents = this.currentValue.value.allComponents;
            for (const channelAddress of channelAddresses) {
                const ca = channelAddress.toString();
                allComponents[ca] = currentData.channel[ca];
            }

            this.currentValue.next({ allComponents: allComponents });
            this.lastUpdated.set(new Date());
        });
    }

    ngOnDestroy() {
        this.edge.unsubscribeFromChannels(this.websocket, this.subscribedChannelAddresses);
        this.stopOnDestroy.next();
        this.stopOnDestroy.complete();
    }

    public unsubscribeFromChannels(channels: ChannelAddress[]) {
        this.lastUpdated.set(null);
        this.edge.unsubscribeFromChannels(this.websocket, channels);
    }

    public override refresh(ev: RefresherCustomEvent) {
        AppService.handleRefresh();
    }
}
