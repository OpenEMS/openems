import { Component, Input } from '@angular/core';
import { Edge, Service, EdgeConfig } from '../../../../../shared/shared';
import { ModalController } from '@ionic/angular';

@Component({
    selector: ConsumptionModalComponent.SELECTOR,
    templateUrl: './modal.component.html'
})
export class ConsumptionModalComponent {

    private static readonly SELECTOR = "consumption-modal";

    @Input() public edge: Edge;
    @Input() public evcss: EdgeConfig.Component[];
    @Input() public consumptionMeters: EdgeConfig.Component[];
    @Input() public evcsSumOfChargePower: number;
    @Input() public otherPower: number;

    public config: EdgeConfig = null;

    constructor(
        public service: Service,
        public modalCtrl: ModalController
    ) { }
}