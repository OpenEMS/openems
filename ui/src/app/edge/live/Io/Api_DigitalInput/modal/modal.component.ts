import { Component, Input } from '@angular/core';
import { Edge, Service, EdgeConfig } from '../../../../../shared/shared';
import { ModalController } from '@ionic/angular';

@Component({
    selector: 'Io_Api_DigitalInputModal',
    templateUrl: './modal.component.html'
})
export class Io_Api_DigitalInput_ModalComponent {

    @Input() public edge: Edge;
    @Input() public ioComponents: EdgeConfig.Component[];

    public config: EdgeConfig = null;

    constructor(
        public service: Service,
        public modalCtrl: ModalController
    ) { }
}