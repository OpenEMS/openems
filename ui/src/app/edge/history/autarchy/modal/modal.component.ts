import { Component, Input } from '@angular/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ModalController } from '@ionic/angular';
import { Service } from '../../../../shared/shared';

@Component({
    selector: AutarchyModalComponent.SELECTOR,
    templateUrl: './modal.component.html'
})
export class AutarchyModalComponent {

    @Input() public period: DefaultTypes.HistoryPeriod;

    private static readonly SELECTOR = "autarchy-modal";

    constructor(
        public service: Service,
        public modalCtrl: ModalController
    ) { }

    ngOnInit() {
    }
}