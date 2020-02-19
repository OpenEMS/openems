import { Component, Input } from '@angular/core';
import { Service, EdgeConfig } from '../../../../../shared/shared';
import { ModalController } from '@ionic/angular';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';

@Component({
    selector: SymmetricPeakshavingModalComponent.SELECTOR,
    templateUrl: './modal.component.html'
})
export class SymmetricPeakshavingModalComponent {

    @Input() public period: DefaultTypes.HistoryPeriod;
    @Input() public component: EdgeConfig.Component;

    private static readonly SELECTOR = "autarchy-modal";

    constructor(
        public service: Service,
        public modalCtrl: ModalController
    ) { }

    ngOnInit() {
    }
}