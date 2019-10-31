import { Component, Input } from '@angular/core';
import { Service, EdgeConfig } from '../../../../shared/shared';
import { ModalController } from '@ionic/angular';

@Component({
    selector: ChpSocModalComponent.SELECTOR,
    templateUrl: './modal.component.html'
})
export class ChpSocModalComponent {

    @Input() public controller: EdgeConfig.Component;

    private static readonly SELECTOR = "chpsoc-modal";

    constructor(
        public service: Service,
        public modalCtrl: ModalController
    ) { }

    ngOnInit() {
    }
}