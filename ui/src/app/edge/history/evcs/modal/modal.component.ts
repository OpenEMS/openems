import { Component } from '@angular/core';
import { Service } from '../../../../shared/shared';
import { ModalController } from '@ionic/angular';

@Component({
    selector: EvcsModalComponent.SELECTOR,
    templateUrl: './modal.component.html'
})
export class EvcsModalComponent {

    private static readonly SELECTOR = "evcs-modal";

    constructor(
        public service: Service,
        public modalCtrl: ModalController
    ) { }

    ngOnInit() {
    }
}