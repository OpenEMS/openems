import { Component, Input } from '@angular/core';
import { Edge, Service, EdgeConfig } from '../../../../shared/shared';
import { ModalController } from '@ionic/angular';

@Component({
    selector: DigitalInputModalComponent.SELECTOR,
    templateUrl: './modal.component.html'
})
export class DigitalInputModalComponent {

    private static readonly SELECTOR = "digitalinput-modal";

    @Input() public edge: Edge;
    @Input() public ioComponents: EdgeConfig.Component[];

    public config: EdgeConfig = null;

    constructor(
        public service: Service,
        public modalCtrl: ModalController
    ) { }
}