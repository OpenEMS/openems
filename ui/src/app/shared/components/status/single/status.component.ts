// @ts-strict-ignore
import { Component, OnDestroy, OnInit } from "@angular/core";
import { ModalController } from "@ionic/angular";
import { Subject } from "rxjs";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { GetStateChannelsOfComponentRequest } from "src/app/shared/jsonrpc/request/getStateChannelsOfComponentRequest";
import { GetChannelsOfComponentResponse } from "src/app/shared/jsonrpc/response/getChannelsOfComponentResponse";
import { ChannelAddress, EdgePermission, Service, Websocket } from "src/app/shared/shared";

import { Edge } from "../../edge/edge";
import { CategorizedComponents, EdgeConfig } from "../../edge/edgeconfig";

@Component({
    selector: STATUS_SINGLE_COMPONENT.SELECTOR,
    templateUrl: "./STATUS.COMPONENT.HTML",
    standalone: false,
})
export class StatusSingleComponent implements OnInit, OnDestroy {
    private static readonly SELECTOR = "statussingle";

    public subscribedInfoChannels: ChannelAddress[] = [];
    public onInfoChannels: ChannelAddress[] = [];
    public edge?: Edge;
    public config: EdgeConfig;
    public components: CategorizedComponents[];
    protected channels: { [componentId: string]: { [channelId: string]: { text: string, level: string } } } = {};

    private stopOnDestroy: Subject<void> = new Subject<void>();

    constructor(
        public modalCtrl: ModalController,
        public service: Service,
        private websocket: Websocket,
    ) { }

    ngOnDestroy() {
        THIS.EDGE?.unsubscribeChannels(THIS.WEBSOCKET, STATUS_SINGLE_COMPONENT.SELECTOR);
        THIS.STOP_ON_DESTROY.NEXT();
        THIS.STOP_ON_DESTROY.COMPLETE();
    }

    async ngOnInit() {
        THIS.CONFIG = await THIS.SERVICE.GET_CONFIG();
        THIS.COMPONENTS = THIS.CONFIG.LIST_ACTIVE_COMPONENTS([], THIS.SERVICE.TRANSLATE);
        THIS.COMPONENTS.FOR_EACH(categorizedComponent => {
            CATEGORIZED_COMPONENT.COMPONENTS.FOR_EACH(component => {
                // sets all arrow buttons to standard position (folded)
                component["showProperties"] = false;
                THIS.SUBSCRIBED_INFO_CHANNELS.PUSH(
                    new ChannelAddress(COMPONENT.ID, "State"),
                );
            });
        });
        //need to subscribe on currentedge because component is opened by APP.COMPONENT
        THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {
            THIS.EDGE = edge;
            EDGE.SUBSCRIBE_CHANNELS(THIS.WEBSOCKET, STATUS_SINGLE_COMPONENT.SELECTOR, THIS.SUBSCRIBED_INFO_CHANNELS);
        });
    }

    public async subscribeInfoChannels(component: EDGE_CONFIG.COMPONENT) {
        const channels = await THIS.GET_STATE_CHANNELS(COMPONENT.ID);
        for (const key of OBJECT.KEYS(channels)) {
            const channelAddress = new ChannelAddress(COMPONENT.ID, key);
            THIS.SUBSCRIBED_INFO_CHANNELS.PUSH(channelAddress);
            THIS.ON_INFO_CHANNELS.PUSH(channelAddress);
        }
        THIS.CHANNELS[COMPONENT.ID] = channels;
        THIS.EDGE?.subscribeChannels(THIS.WEBSOCKET, STATUS_SINGLE_COMPONENT.SELECTOR, THIS.SUBSCRIBED_INFO_CHANNELS);
    }

    public unsubscribeInfoChannels(component: EDGE_CONFIG.COMPONENT) {
        delete THIS.CHANNELS[COMPONENT.ID];
        //removes unsubscribed elements from subscribedInfoChannels array
        THIS.ON_INFO_CHANNELS.FOR_EACH(onInfoChannel => {
            THIS.SUBSCRIBED_INFO_CHANNELS.FOR_EACH((subChannel, index) => {
                if (ON_INFO_CHANNEL.CHANNEL_ID == SUB_CHANNEL.CHANNEL_ID && COMPONENT.ID == SUB_CHANNEL.COMPONENT_ID) {
                    THIS.SUBSCRIBED_INFO_CHANNELS.SPLICE(index, 1);
                }
            });
        });
        //clear onInfoChannels Array
        THIS.ON_INFO_CHANNELS = THIS.ON_INFO_CHANNELS.FILTER((channel) => CHANNEL.COMPONENT_ID != COMPONENT.ID);
        THIS.EDGE?.subscribeChannels(THIS.WEBSOCKET, STATUS_SINGLE_COMPONENT.SELECTOR, THIS.SUBSCRIBED_INFO_CHANNELS);
    }

    private getStateChannels(componentId: string): Promise<typeof THIS.CHANNELS["componentId"]> {
        return new Promise((resolve, reject) => {
            if (EDGE_PERMISSION.HAS_CHANNELS_IN_EDGE_CONFIG(THIS.EDGE)) {
                const channels: typeof THIS.CHANNELS["componentId"] = {};
                for (const [key, value] of OBJECT.ENTRIES(THIS.CONFIG.COMPONENTS[componentId].channels)) {

                    // show only state channels
                    if (VALUE.CATEGORY !== "STATE") {
                        continue;
                    }

                    channels[key] = { text: VALUE.TEXT, level: VALUE.LEVEL };
                }
                resolve(channels);
                return;
            }

            THIS.EDGE.SEND_REQUEST(THIS.WEBSOCKET, new ComponentJsonApiRequest({
                componentId: "_componentManager",
                payload: new GetStateChannelsOfComponentRequest({ componentId: componentId }),
            })).then((response: GetChannelsOfComponentResponse) => {
                const channels: typeof THIS.CHANNELS["componentId"] = {};
                for (const item of RESPONSE.RESULT.CHANNELS) {
                    channels[ITEM.ID] = { text: ITEM.TEXT, level: ITEM.LEVEL };
                }
                resolve(channels);
            }).catch(reject);
        });
    }

}
