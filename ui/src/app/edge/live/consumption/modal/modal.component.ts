import { Component, Input } from '@angular/core';
import { Edge, Service, Websocket } from '../../../../shared/shared';
import { ModalController } from '@ionic/angular';

@Component({
    selector: 'consumption-modal',
    templateUrl: './modal.component.html'
})
export class ConsumptionModalComponent {

    private static readonly SELECTOR = "consumption-modal";

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
            this.edge.unsubscribeChannels(this.websocket, ConsumptionModalComponent.SELECTOR);
        }
    }
}
