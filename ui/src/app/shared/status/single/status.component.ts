// @ts-strict-ignore
import { Component, OnDestroy, OnInit } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ChannelAddress, Edge, EdgePermission, Service, Websocket } from '../../../shared/shared';
import { CategorizedComponents, EdgeConfig } from '../../edge/edgeconfig';
import { ComponentJsonApiRequest } from '../../jsonrpc/request/componentJsonApiRequest';
import { GetStateChannelsOfComponentRequest } from '../../jsonrpc/request/getStateChannelsOfComponentRequest';
import { GetChannelsOfComponentResponse } from '../../jsonrpc/response/getChannelsOfComponentResponse';

@Component({
    selector: StatusSingleComponent.SELECTOR,
    templateUrl: './status.component.html',
})
export class StatusSingleComponent implements OnInit, OnDestroy {

    private stopOnDestroy: Subject<void> = new Subject<void>();

    public edge?: Edge;
    public config: EdgeConfig;
    public components: CategorizedComponents[];
    protected channels: { [componentId: string]: { [channelId: string]: { text: string, level: string } } } = {};
    public subscribedInfoChannels: ChannelAddress[] = [];
    public onInfoChannels: ChannelAddress[] = [];

    private static readonly SELECTOR = "statussingle";

    constructor(
        public modalCtrl: ModalController,
        public service: Service,
        private websocket: Websocket,
    ) { }

    async ngOnInit() {
        this.config = await this.service.getConfig();
        this.components = this.config.listActiveComponents();
        this.components.forEach(categorizedComponent => {
            categorizedComponent.components.forEach(component => {
                // sets all arrow buttons to standard position (folded)
                component['showProperties'] = false;
                this.subscribedInfoChannels.push(
                    new ChannelAddress(component.id, 'State'),
                );
            });
        });
        //need to subscribe on currentedge because component is opened by app.component
        this.service.currentEdge.pipe(takeUntil(this.stopOnDestroy)).subscribe(edge => {
            this.edge = edge;
            edge.subscribeChannels(this.websocket, StatusSingleComponent.SELECTOR, this.subscribedInfoChannels);
        });
    }

    public async subscribeInfoChannels(component: EdgeConfig.Component) {
        const channels = await this.getStateChannels(component.id);
        for (const key of Object.keys(channels)) {
            const channelAddress = new ChannelAddress(component.id, key);
            this.subscribedInfoChannels.push(channelAddress);
            this.onInfoChannels.push(channelAddress);
        }
        this.channels[component.id] = channels;
        this.edge?.subscribeChannels(this.websocket, StatusSingleComponent.SELECTOR, this.subscribedInfoChannels);
    }

    public unsubscribeInfoChannels(component: EdgeConfig.Component) {
        delete this.channels[component.id];
        //removes unsubscribed elements from subscribedInfoChannels array
        this.onInfoChannels.forEach(onInfoChannel => {
            this.subscribedInfoChannels.forEach((subChannel, index) => {
                if (onInfoChannel.channelId == subChannel.channelId && component.id == subChannel.componentId) {
                    this.subscribedInfoChannels.splice(index, 1);
                }
            });
        });
        //clear onInfoChannels Array
        this.onInfoChannels = this.onInfoChannels.filter((channel) => channel.componentId != component.id);
        this.edge?.subscribeChannels(this.websocket, StatusSingleComponent.SELECTOR, this.subscribedInfoChannels);
    }

    private getStateChannels(componentId: string): Promise<typeof this.channels['componentId']> {
        return new Promise((resolve, reject) => {
            if (EdgePermission.hasChannelsInEdgeConfig(this.edge)) {
                const channels: typeof this.channels['componentId'] = {};
                for (const [key, value] of Object.entries(this.config.components[componentId].channels)) {

                    // show only state channels
                    if (value.category !== "STATE") {
                        continue;
                    }

                    channels[key] = { text: value.text, level: value.level };
                }
                resolve(channels);
                return;
            }

            this.edge.sendRequest(this.websocket, new ComponentJsonApiRequest({
                componentId: '_componentManager',
                payload: new GetStateChannelsOfComponentRequest({ componentId: componentId }),
            })).then((response: GetChannelsOfComponentResponse) => {
                const channels: typeof this.channels['componentId'] = {};
                for (const item of response.result.channels) {
                    channels[item.id] = { text: item.text, level: item.level };
                }
                resolve(channels);
            }).catch(reject);
        });
    }

    ngOnDestroy() {
        this.edge?.unsubscribeChannels(this.websocket, StatusSingleComponent.SELECTOR);
        this.stopOnDestroy.next();
        this.stopOnDestroy.complete();
    }
}
