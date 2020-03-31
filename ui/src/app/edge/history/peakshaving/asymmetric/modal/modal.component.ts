import { Component, Input } from '@angular/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ModalController } from '@ionic/angular';
import { Service, EdgeConfig } from '../../../../../shared/shared';

@Component({
    selector: AsymmetricPeakshavingModalComponent.SELECTOR,
    templateUrl: './modal.component.html'
})
export class AsymmetricPeakshavingModalComponent {

    @Input() public period: DefaultTypes.HistoryPeriod;
    @Input() public component: EdgeConfig.Component;

    private static readonly SELECTOR = "autarchy-modal";

    constructor(
        public service: Service,
        public modalCtrl: ModalController
    ) { }
}