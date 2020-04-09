import { Component, Input } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Service, Utils, EdgeConfig } from '../../../../shared/shared';

@Component({
    selector: HeatingelementModalComponent.SELECTOR,
    templateUrl: './modal.component.html'
})
export class HeatingelementModalComponent {

    @Input() public component: EdgeConfig.Component;

    private static readonly SELECTOR = "heatingelement-modal";

    constructor(
        public service: Service,
        public modalCtrl: ModalController
    ) { }

    ngOnInit() { }
}