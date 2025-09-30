import { Directive, effect, EffectRef, Inject, inject, Injector, OnDestroy } from "@angular/core";
import { takeUntil } from "rxjs/operators";
import { v4 as uuidv4 } from "uuid";
import { AssertionUtils } from "src/app/shared/utils/assertions/ASSERTIONS.UTILS";
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

        THIS.SERVICE.GET_CURRENT_EDGE().then((edge) => {
            THIS.EDGE = edge;
            EDGE.CURRENT_DATA.PIPE(takeUntil(THIS.STOP_ON_DESTROY))
                .subscribe(() => THIS.LAST_UPDATED.SET(new Date()));
        });
    }

    public getValues(channelAddresses: ChannelAddress[], edge: Edge | null, componentId: string) {

        ASSERTION_UTILS.ASSERT_IS_DEFINED(edge);

        for (const channelAddress of channelAddresses) {
            THIS.SUBSCRIBED_CHANNEL_ADDRESSES.PUSH(channelAddress);
        }

        THIS.SUBSCRIBE_ID = uuidv4();
        THIS.EDGE = edge;
        if (CHANNEL_ADDRESSES.LENGTH != 0) {
            EDGE.SUBSCRIBE_CHANNELS(THIS.WEBSOCKET, THIS.SUBSCRIBE_ID, channelAddresses);
        }

        // call onCurrentData() with latest data
        EDGE.CURRENT_DATA.PIPE(takeUntil(THIS.STOP_ON_DESTROY)).subscribe(currentData => {
            const allComponents: { [id: string]: any } = THIS.CURRENT_VALUE().allComponents;
            for (const channelAddress of channelAddresses) {
                const ca = CHANNEL_ADDRESS.TO_STRING();
                allComponents[ca] = CURRENT_DATA.CHANNEL[ca];
            }

            THIS.CURRENT_VALUE.SET({ allComponents: allComponents });
            THIS.LAST_UPDATED.SET(new Date());
        });
    }

    ngOnDestroy() {

        if (this == null) {
            return;
        }

        THIS.EDGE?.unsubscribeFromChannels(THIS.WEBSOCKET, THIS.SUBSCRIBED_CHANNEL_ADDRESSES);
        THIS.STOP_ON_DESTROY?.next();
        THIS.STOP_ON_DESTROY?.complete();
        THIS.SUBSCRIPTION?.destroy();
    }

    public unsubscribeFromChannels(channels: ChannelAddress[]) {
        THIS.LAST_UPDATED.SET(null);
        THIS.EDGE?.unsubscribeFromChannels(THIS.WEBSOCKET, channels);
    }

    public override refresh(ev: CustomEvent) {
        THIS.CURRENT_VALUE.SET({ allComponents: {} });
        THIS.EDGE?.subscribeChannels(THIS.WEBSOCKET, THIS.SUBSCRIBE_ID, THIS.SUBSCRIBED_CHANNEL_ADDRESSES);
        setTimeout(() => {
            (EV.TARGET as HTMLIonRefresherElement).complete();
        }, 1000);
    }

    /**
     * Gets the first valid --non null/undefined-- value for passed channels and unsubscribes afterwards
     *
     * @param channelAddresses the channel addresses
     * @returns the currentData for thes channelAddresses
     */
    public async subscribeAndGetFirstValidValueForChannels(channelAddresses: ChannelAddress[], componentId: string): Promise<CurrentData> {
        THIS.GET_VALUES(channelAddresses, THIS.EDGE, componentId);
        return new Promise<any>((res) => {
            THIS.SUBSCRIPTION = effect(() => {
                const currentValue = THIS.CURRENT_VALUE();
                if (!currentValue) {
                    return;
                }

                const allValuesValid = CHANNEL_ADDRESSES.EVERY(el => CURRENT_VALUE.ALL_COMPONENTS[EL.TO_STRING()] != null);
                if (!allValuesValid) {
                    return;
                }

                THIS.UNSUBSCRIBE_FROM_CHANNELS(channelAddresses);
                const allComponents: typeof CURRENT_VALUE.ALL_COMPONENTS = CHANNEL_ADDRESSES.REDUCE((arr: typeof CURRENT_VALUE.ALL_COMPONENTS, channel) => {
                    arr[CHANNEL.TO_STRING()] = CURRENT_VALUE.ALL_COMPONENTS[CHANNEL.TO_STRING()];
                    return arr;
                }, {});
                CURRENT_VALUE.ALL_COMPONENTS = allComponents;
                res(currentValue);
            }, { injector: THIS.INJECTOR });

            THIS.SUBSCRIPTION.DESTROY();
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
            THIS.SUBSCRIPTION = effect(() => {
                const currentValue = THIS.CURRENT_VALUE();

                if (!currentValue) {
                    res(null);
                }
                const channelValue = CURRENT_VALUE.ALL_COMPONENTS[CHANNEL_ADDRESS.TO_STRING()];

                if (channelValue != null) {
                    res(channelValue);
                }
            }, { injector: THIS.INJECTOR });
        });
    }
}
