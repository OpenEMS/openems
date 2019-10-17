import { Component } from '@angular/core';
import { Service } from '../../../../shared/shared';
import { ModalController } from '@ionic/angular';

@Component({
    selector: SelfconsumptionModalComponent.SELECTOR,
    templateUrl: './modal.component.html'
})
export class SelfconsumptionModalComponent {

    private static readonly SELECTOR = "selfconsumption-modal";

    constructor(
        public service: Service,
        public modalCtrl: ModalController
    ) { }

    ngOnInit() {
    }
}