import { Component } from '@angular/core';
import { Service } from '../../../../shared/shared';
import { ModalController } from '@ionic/angular';

@Component({
    selector: ChannelthresholdModalComponent.SELECTOR,
    templateUrl: './modal.component.html'
})
export class ChannelthresholdModalComponent {

    private static readonly SELECTOR = "channelthreshold-modal";

    constructor(
        public service: Service,
        public modalCtrl: ModalController
    ) { }

    ngOnInit() {
    }
}