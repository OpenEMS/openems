import { Component } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Service } from '../../../../shared/shared';

@Component({
    selector: AwattarModalComponent.SELECTOR,
    templateUrl: './modal.component.html'
})
export class AwattarModalComponent {

    private static readonly SELECTOR = "awattar-modal";

    constructor(
        public service: Service,
        public modalCtrl: ModalController,
    ) { }

}