import { Component, OnDestroy, OnInit } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ChannelAddress, Edge, Service, Websocket } from '../../../shared/shared';
import { CategorizedComponents, EdgeConfig } from '../../edge/edgeconfig';

@Component({
    selector: StatusSingleComponent.SELECTOR,
    templateUrl: './status.component.html'
})
export class StatusSingleComponent implements OnInit, OnDestroy {

    private stopOnDestroy: Subject<void> = new Subject<void>();

    public edge: Edge;
    public config: EdgeConfig;
    public components: CategorizedComponents[];
    public subscribedInfoChannels: ChannelAddress[] = [];
    public onInfoChannels: ChannelAddress[] = [];

    private static readonly SELECTOR = "statussingle";

    constructor(
        public modalCtrl: ModalController,
        public service: Service,
        private websocket: Websocket,
    ) { }

    ngOnInit() {
        this.service.getConfig().then(config => {
            this.config = config;
            let categorizedComponentIds: string[] = []
            this.components = config.listActiveComponents(categorizedComponentIds);
            this.components.forEach(categorizedComponent => {
                categorizedComponent.components.forEach(component => {
                    // sets all arrow buttons to standard position (folded)
                    component['showProperties'] = false;
                    this.subscribedInfoChannels.push(
                        new ChannelAddress(component.id, 'State')
                    )
                })
            })
            //need to subscribe on currentedge because component is opened by app.component
            this.service.currentEdge.pipe(takeUntil(this.stopOnDestroy)).subscribe(edge => {
                this.edge = edge;
                edge.subscribeChannels(this.websocket, StatusSingleComponent.SELECTOR, this.subscribedInfoChannels);
            })
        })
    }

    public subscribeInfoChannels(component: EdgeConfig.Component) {
        Object.keys(component.channels).forEach(channel => {
            if (component.channels[channel]['level']) {
                this.subscribedInfoChannels.push(new ChannelAddress(component.id, channel))
                this.onInfoChannels.push(new ChannelAddress(component.id, channel))
            }
        });
        if (this.edge) {
            this.edge.subscribeChannels(this.websocket, StatusSingleComponent.SELECTOR, this.subscribedInfoChannels);
        }
    }

    public unsubscribeInfoChannels(component: EdgeConfig.Component) {
        //removes unsubscribed elements from subscribedInfoChannels array
        this.onInfoChannels.forEach(onInfoChannel => {
            this.subscribedInfoChannels.forEach((subChannel, index) => {
                if (onInfoChannel.channelId == subChannel.channelId && component.id == subChannel.componentId) {
                    this.subscribedInfoChannels.splice(index, 1);
                }
            });
        })
        //clear onInfoChannels Array
        this.onInfoChannels = this.onInfoChannels.filter((channel) => channel.componentId != component.id)
        if (this.edge) {
            this.edge.subscribeChannels(this.websocket, StatusSingleComponent.SELECTOR, this.subscribedInfoChannels);
        }
    }

    ngOnDestroy() {
        this.stopOnDestroy.next();
        this.stopOnDestroy.complete();
    }
}