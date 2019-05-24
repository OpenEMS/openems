import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Service, Websocket } from '../shared';
import { ModalController } from '@ionic/angular';

@Component({
    selector: 'pickdate',
    templateUrl: './pickdate.component.html'
})
export class PickDateComponent {

    constructor(
        public service: Service,
        private websocket: Websocket,
        private route: ActivatedRoute,
        private modalController: ModalController
    ) { }

    ngOnInit() { }

    ngOnDestroy() { }
}
