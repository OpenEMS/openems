import { Component, Input } from '@angular/core';
import { Service } from '../../../../shared/shared';
import { ModalController } from '@ionic/angular';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';

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