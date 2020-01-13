import { Component } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Service } from '../../../../shared/shared';

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