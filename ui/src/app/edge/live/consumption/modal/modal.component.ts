import { Component, Input } from '@angular/core';
import { Edge, Service, Websocket, EdgeConfig } from '../../../../shared/shared';
import { ModalController } from '@ionic/angular';
import { ActivatedRoute } from '@angular/router';

@Component({
    selector: 'consumption-modal',
    templateUrl: './modal.component.html'
})
export class ConsumptionModalComponent {

    private static readonly SELECTOR = "consumption-modal";

    @Input() edge: Edge;
    @Input() evcsComponents: EdgeConfig.Component[];
    @Input() currentTotalChargingPower: Function;
    @Input() sumOfChannel: Function;

    public config: EdgeConfig = null;

    constructor(
        public service: Service,
        private websocket: Websocket,
        public modalCtrl: ModalController,
        private route: ActivatedRoute,
    ) { }

    ngOnInit() {
    }

    ngOnDestroy() {
        if (this.edge != null) {
            this.edge.unsubscribeChannels(this.websocket, ConsumptionModalComponent.SELECTOR);
        }
    }
}