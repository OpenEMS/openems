import { Component, Input } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Service, EdgeConfig } from '../../../../shared/shared';

@Component({
    selector: ChpSocModalComponent.SELECTOR,
    templateUrl: './modal.component.html'
})
export class ChpSocModalComponent {

    @Input() public component: EdgeConfig.Component;

    private static readonly SELECTOR = "chpsoc-modal";

    constructor(
        public service: Service,
        public modalCtrl: ModalController
    ) { }

    ngOnInit() {
    }
}