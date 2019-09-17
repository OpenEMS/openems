import { Component, Input, Output } from '@angular/core';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from '../../../shared/shared';
import { ActivatedRoute } from '@angular/router';
import { ChpsocModalComponent } from './chpsoc-modal/chpsoc-modal.page';
import { ModalController } from '@ionic/angular';

@Component({
    selector: ChpSocComponent.SELECTOR,
    templateUrl: './chpsoc.component.html'
})
export class ChpSocComponent {

    private static readonly SELECTOR = "chpsoc";

    @Input() private componentId: string;

    private edge: Edge = null;

    public controller: EdgeConfig.Component = null;
    public inputChannel: ChannelAddress = null;
    public outputChannel: ChannelAddress = null;
    public thresholdEnd: number;

    constructor(
        public service: Service,
        private websocket: Websocket,
        private route: ActivatedRoute,
        public modalController: ModalController
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
            this.service.getConfig().then(config => {
                this.controller = config.components[this.componentId];
                this.thresholdEnd = this.controller.properties['highThreshold'] - this.controller.properties['lowThreshold'];
                this.outputChannel = ChannelAddress.fromString(
                    this.controller.properties['outputChannelAddress']);
                this.inputChannel = ChannelAddress.fromString(
                    this.controller.properties['inputChannelAddress']);
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
                controller: this.controller,
                edge: this.edge,
                componentId: this.componentId,
            }
        });
        return await modal.present();
    }
}