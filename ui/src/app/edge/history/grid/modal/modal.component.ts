import { Component } from '@angular/core';
import { Service } from '../../../../shared/shared';
import { ModalController } from '@ionic/angular';

@Component({
    selector: GridModalComponent.SELECTOR,
    templateUrl: './modal.component.html'
})
export class GridModalComponent {

    private static readonly SELECTOR = "grid-modal";

    constructor(
        public service: Service,
        public modalCtrl: ModalController
    ) { }

    ngOnInit() {
    }
}