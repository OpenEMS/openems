import { Component } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Service } from '../../../../shared/shared';

@Component({
    selector: 'selfconsumption-modal',
    templateUrl: './modal.component.html'
})
export class SelfconsumptionModalComponent {

    constructor(
        public service: Service,
        public modalCtrl: ModalController,
    ) { }
}