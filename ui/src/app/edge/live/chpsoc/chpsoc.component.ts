import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from '../../../shared/shared';
import { ChpsocModalComponent } from './chpsoc-modal/modal.page';
import { Component, Input } from '@angular/core';
import { ModalController } from '@ionic/angular';

@Component({
    selector: ChpSocComponent.SELECTOR,
    templateUrl: './chpsoc.component.html'
})
export class ChpSocComponent {

    private static readonly SELECTOR = "chpsoc";

    @Input() private componentId: string;

    private edge: Edge = null;

    public component: EdgeConfig.Component = null;
    public inputChannel: ChannelAddress = null;
    public outputChannel: ChannelAddress = null;

    constructor(
        private route: ActivatedRoute,
        private websocket: Websocket,
        public modalController: ModalController,
        public service: Service,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
            this.service.getConfig().then(config => {
                this.component = config.components[this.componentId];
                this.outputChannel = ChannelAddress.fromString(
                    this.component.properties['outputChannelAddress']);
                this.inputChannel = ChannelAddress.fromString(
                    this.component.properties['inputChannelAddress']);
                edge.subscribeChannels(this.websocket, ChpSocComponent.SELECTOR + this.componentId, [
                    this.outputChannel,
                    this.inputChannel
                ]);
            });
        });
    }

    ngOnDestroy() {
        if (this.edge != null) {
            this.edge.unsubscribeChannels(this.websocket, ChpSocComponent.SELECTOR + this.componentId);
        }
    }


    async presentModal() {
        const modal = await this.modalController.create({
            component: ChpsocModalComponent,
            componentProps: {
                component: this.component,
                edge: this.edge,
                outputChannel: this.outputChannel,
                inputChannel: this.inputChannel
            }
        });
        return await modal.present();
    }
}