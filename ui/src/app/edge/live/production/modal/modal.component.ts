import { Component, Input, OnDestroy } from '@angular/core';
import { ChannelAddress, Edge, Service, Websocket, EdgeConfig, Utils } from '../../../../shared/shared';
import { ModalController } from '@ionic/angular';

@Component({
    selector: 'production-modal',
    templateUrl: './modal.component.html'
})
// FIXME note: you should implement OnDestroy if you need ngOnDestroy
export class ProductionModalComponent implements OnDestroy {

    private static readonly SELECTOR = "production-modal";

    // FIXME note: this is how you can get a reference to the Utils method. Another option is via decorator; see this: https://stackoverflow.com/questions/41857047/call-static-function-from-angular2-template
    public isLastElement = Utils.isLastElement;

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

    // FIXME Do you really need ngOnDestroy? I don't see you subscribing anywhere.
    ngOnDestroy() {
        if (this.edge != null) {
            this.edge.unsubscribeChannels(this.websocket, ProductionModalComponent.SELECTOR);
        }
    }
}