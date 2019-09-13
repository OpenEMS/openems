import { Component, Input, OnDestroy } from '@angular/core';
import { ChannelAddress, Edge, Service, Websocket, EdgeConfig, Utils } from '../../../../shared/shared';
import { ModalController } from '@ionic/angular';

@Component({
    selector: 'production-modal',
    templateUrl: './modal.component.html'
})
export class ProductionModalComponent {

    private static readonly SELECTOR = "production-modal";

    // referene to the Utils method to access via html
    public isLastElement = Utils.isLastElement;

    @Input() edge: Edge;
    @Input() config: EdgeConfig;
    @Input() productionMeterComponents: EdgeConfig.Component[];
    @Input() chargerComponents: EdgeConfig.Component[];

    constructor(
        public service: Service,
        public modalCtrl: ModalController,
    ) { }

    ngOnInit() { }
}