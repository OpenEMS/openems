import { Component, Input } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Service, Edge } from '../../../../../shared/shared';

@Component({
    selector: HomeElectricModalComponent.SELECTOR,
    templateUrl: './modal.component.html'
})
export class HomeElectricModalComponent {

    private static readonly SELECTOR = "homelectric-modal";

    @Input() public edge: Edge;
    constructor(

        public modalCtrl: ModalController,
        public service: Service,
    ) { }
}