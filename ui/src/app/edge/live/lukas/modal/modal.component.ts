import { Component } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Service } from '../../../../shared/shared';

@Component({
    selector: LukasModalComponent.SELECTOR,
    templateUrl: './modal.component.html',
    styleUrls: ['./modal.component.css']
})
export class LukasModalComponent {

    private static readonly SELECTOR = "lukas-modal";

    constructor(
        public modalCtrl: ModalController,
        public service: Service,
    ) { }
}