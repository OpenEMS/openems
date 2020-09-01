import { Component, Input } from '@angular/core';
import { Edge, EdgeConfig, Service, Utils } from '../../../../shared/shared';
import { ModalController } from '@ionic/angular';

@Component({
    selector: 'production-modal',
    templateUrl: './modal.component.html'
})
export class ProductionModalComponent {


    // referene to the Utils method to access via html
    public isLastElement = Utils.isLastElement;

    @Input() public edge: Edge | null = null;
    @Input() public config: EdgeConfig | null = null;
    @Input() public productionMeterComponents: EdgeConfig.Component[] = [];
    @Input() public chargerComponents: EdgeConfig.Component[] = [];

    constructor(
        public service: Service,
        public modalCtrl: ModalController,
    ) { }
}