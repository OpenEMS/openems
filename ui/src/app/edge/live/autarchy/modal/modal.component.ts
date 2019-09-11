import { Component, Input } from '@angular/core';
import { ChannelAddress, Edge, Service, Websocket, EdgeConfig, Utils } from '../../../../shared/shared';
import { ModalController } from '@ionic/angular';

@Component({
    selector: 'autarchy-modal',
    templateUrl: './modal.component.html'
})
export class AutarchyModalComponent {

    private static readonly SELECTOR = "autarchy-modal";

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
            this.edge.unsubscribeChannels(this.websocket, AutarchyModalComponent.SELECTOR);
        }
    }
}