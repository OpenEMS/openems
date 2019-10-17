import { Component } from '@angular/core';
import { Service } from '../../../../shared/shared';
import { ModalController } from '@ionic/angular';

@Component({
    selector: ChpSocModalComponent.SELECTOR,
    templateUrl: './modal.component.html'
})
export class ChpSocModalComponent {

    private static readonly SELECTOR = "chpsoc-modal";

    constructor(
        public service: Service,
        public modalCtrl: ModalController
    ) { }

    ngOnInit() {
    }
}