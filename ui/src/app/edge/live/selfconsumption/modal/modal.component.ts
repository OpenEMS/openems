import { Component, Input } from '@angular/core';
import { ChannelAddress, Edge, Service, Websocket, EdgeConfig, Utils } from '../../../../shared/shared';
import { ModalController } from '@ionic/angular';

@Component({
    selector: 'selfconsumption-modal',
    templateUrl: './modal.component.html'
})
export class SelfconsumptionModalComponent {

    private static readonly SELECTOR = "production-modal";

    @Input() edge: Edge;

    constructor(
        public service: Service,
        private websocket: Websocket,
        public modalCtrl: ModalController,
    ) { }

    ngOnInit() {
    }

    ngOnDestroy() {
        if (this.edge != null) {
            this.edge.unsubscribeChannels(this.websocket, SelfconsumptionModalComponent.SELECTOR);
        }
    }
}