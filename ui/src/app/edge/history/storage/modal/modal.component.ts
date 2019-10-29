import { Component, Input } from '@angular/core';
import { Service, EdgeConfig } from '../../../../shared/shared';
import { ModalController } from '@ionic/angular';

@Component({
    selector: StorageModalComponent.SELECTOR,
    templateUrl: './modal.component.html'
})
export class StorageModalComponent {

    @Input() public config: EdgeConfig;

    private static readonly SELECTOR = "storage-modal";

    constructor(
        public service: Service,
        public modalCtrl: ModalController
    ) { }

    ngOnInit() {
        console.log("ALOHAOHOAHOAHOA", this.config.getComponentsImplementingNature("io.openems.edge.ess.api.SymmetricEss"))
    }

    public getChartHeight(): number {
        return window.innerHeight / 2.5;
    }
}