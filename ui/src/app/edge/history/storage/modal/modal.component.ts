import { Component } from '@angular/core';
import { Service } from '../../../../shared/shared';
import { ModalController } from '@ionic/angular';

@Component({
    selector: StorageModalComponent.SELECTOR,
    templateUrl: './modal.component.html'
})
export class StorageModalComponent {

    private static readonly SELECTOR = "storage-modal";

    constructor(
        public service: Service,
        public modalCtrl: ModalController
    ) { }

    ngOnInit() {
    }

    public getChartHeight(): number {
        return window.innerHeight / 2.5;
    }
}