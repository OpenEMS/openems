import { Directive, effect, EffectRef, Inject, inject, Injector, OnDestroy } from "@angular/core";
import { takeUntil } from "rxjs/operators";
import { v4 as uuidv4 } from "uuid";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { DataService } from "../../shared/components/shared/dataservice";
import { ChannelAddress, CurrentData, Edge, Service, Websocket } from "../../shared/shared";

@Directive()
export class LiveDataService extends DataService implements OnDestroy {

    private subscribeId: string = uuidv4();
    private subscribedChannelAddresses: ChannelAddress[] = [];
    private subscription: EffectRef | null = null;
    private injector: Injector = inject(Injector);

    constructor(
        @Inject(Websocket) protected websocket: Websocket,
        @Inject(Service) protected service: Service,
    ) {
        super(service);

        this.service.getCurrentEdge().then((edge) => {
            this.edge = edge;
            edge.currentData.pipe(takeUntil(this.stopOnDestroy))
                .subscribe(() => this.lastUpdated.set(new Date()));
        });
    }

    public getValues(channelAddresses: ChannelAddress[], edge: Edge | null, componentId: string) {

        AssertionUtils.assertIsDefined(edge);

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
            const allComponents: { [id: string]: any } = this.currentValue().allComponents;
            for (const channelAddress of channelAddresses) {
                const ca = channelAddress.toString();
                allComponents[ca] = currentData.channel[ca];
            }

            this.currentValue.set({ allComponents: allComponents });
            this.lastUpdated.set(new Date());
        });
    }

    ngOnDestroy() {

        if (this == null) {
            return;
        }

        this.edge?.unsubscribeFromChannels(this.subscribeId, this.websocket, this.subscribedChannelAddresses);
        this.stopOnDestroy?.next();
        this.stopOnDestroy?.complete();
        this.subscription?.destroy();
    }

    public unsubscribeFromChannels(channels: ChannelAddress[]) {
        this.lastUpdated.set(null);
        this.edge?.unsubscribeFromChannels(this.subscribeId, this.websocket, channels);
    }

    public override refresh(ev: CustomEvent) {
        this.currentValue.set({ allComponents: {} });
        this.edge?.subscribeChannels(this.websocket, this.subscribeId, this.subscribedChannelAddresses);
        setTimeout(() => {
            (ev.target as HTMLIonRefresherElement).complete();
        }, 1000);
    }

    /**
     * Gets the first valid --non null/undefined-- value for passed channels and unsubscribes afterwards
     *
     * @param channelAddresses the channel addresses
     * @returns the currentData for thes channelAddresses
     */
    public async subscribeAndGetFirstValidValueForChannels(channelAddresses: ChannelAddress[], componentId: string): Promise<CurrentData> {
        this.getValues(channelAddresses, this.edge, componentId);
        return new Promise<any>((res) => {
            this.subscription = effect(() => {
                const currentValue = this.currentValue();
                if (!currentValue) {
                    return;
                }

                const allValuesValid = channelAddresses.every(el => currentValue.allComponents[el.toString()] != null);
                if (!allValuesValid) {
                    return;
                }

                this.unsubscribeFromChannels(channelAddresses);
                const allComponents: typeof currentValue.allComponents = channelAddresses.reduce((arr: typeof currentValue.allComponents, channel) => {
                    arr[channel.toString()] = currentValue.allComponents[channel.toString()];
                    return arr;
                }, {});
                currentValue.allComponents = allComponents;
                res(currentValue);
            }, { injector: this.injector });

            this.subscription.destroy();
        });
    }

    /**
     * Gets the first valid --non null/undefined-- value for this channel
     *
     * @param channelAddress the channel address
     * @returns a non null/undefined value
     */
    public async getFirstValidValueForChannel<T = any>(channelAddress: ChannelAddress): Promise<T | null> {
        return new Promise<any>((res) => {
            this.subscription = effect(() => {
                const currentValue = this.currentValue();

                if (!currentValue) {
                    res(null);
                }
                const channelValue = currentValue.allComponents[channelAddress.toString()];

                if (channelValue != null) {
                    res(channelValue);
                }
            }, { injector: this.injector });
        });
    }
}
