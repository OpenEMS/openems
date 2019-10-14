import { Component } from '@angular/core';
import { Service } from '../../../../shared/shared';
import { ModalController } from '@ionic/angular';

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