import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { Service, Websocket } from '../../shared';

@Component({
    selector: 'pickdate',
    templateUrl: './pickdate-modal.component.html'
})
export class PickDateModalComponent {

    constructor(
        public service: Service,
        private websocket: Websocket,
        private route: ActivatedRoute,
        private modalController: ModalController
    ) { }

    ngOnInit() { }

    ngOnDestroy() { }
}
