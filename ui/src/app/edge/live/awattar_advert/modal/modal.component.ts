import { Component } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Service } from '../../../../shared/shared';

@Component({
    selector: AwattarAdvertModalComponent.SELECTOR,
    templateUrl: './modal.component.html'
})
export class AwattarAdvertModalComponent {

    private static readonly SELECTOR = "awattar-modal";

    constructor(
        public service: Service,
        public modalCtrl: ModalController,
    ) { }

}