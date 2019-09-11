import { Component, Input } from '@angular/core';
import { ChannelAddress, Edge, Service, Websocket, EdgeConfig, Utils } from '../../../../shared/shared';
import { ModalController } from '@ionic/angular';

@Component({
    selector: 'production-modal',
    templateUrl: './modal.component.html'
})
export class ProductionModalComponent {

    private static readonly SELECTOR = "production-modal";

    @Input() edge: Edge;
    @Input() config: EdgeConfig;
    @Input() productionMeterComponents: EdgeConfig.Component;
    @Input() chargerComponents: EdgeConfig.Component;

    constructor(
        public service: Service,
        private websocket: Websocket,
        public modalCtrl: ModalController,
    ) { }

    ngOnInit() { }

    // ToDo: move to Utils completely *atm not reachable via Utils on html*
    public isLastElement(element, array: any[]) {
        return Utils.isLastElement(element, array);
    }

    ngOnDestroy() {
        if (this.edge != null) {
            this.edge.unsubscribeChannels(this.websocket, ProductionModalComponent.SELECTOR);
        }
    }
}