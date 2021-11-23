import { Component } from '@angular/core';
import { ModalController } from '@ionic/angular';

@Component({
    templateUrl: './modal.component.html'
})
export class SilentPartnershipModalComponent {

    constructor(
        public modalCtrl: ModalController,
    ) { }
}