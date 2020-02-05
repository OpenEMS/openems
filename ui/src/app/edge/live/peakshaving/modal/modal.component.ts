import { Component } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Service } from '../../../../shared/shared';

@Component({
    selector: SymmetricPeakshavingModalComponent.SELECTOR,
    templateUrl: './modal.component.html'
})
export class SymmetricPeakshavingModalComponent {

    private static readonly SELECTOR = "symmetricpeakshaving-modal";

    constructor(
        public service: Service,
        public modalCtrl: ModalController,
    ) { }
}