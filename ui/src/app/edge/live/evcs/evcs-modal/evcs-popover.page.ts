import { Component, Input } from '@angular/core';
import { Edge, Service, Websocket, EdgeConfig } from '../../../../shared/shared';
import { ModalController } from '@ionic/angular';
import { ActivatedRoute } from '@angular/router';

@Component({
    selector: 'evcs-popover',
    templateUrl: './evcs-popover.page.html'
})
export class EvcsPopoverComponent {

    private static readonly SELECTOR = "evcs-popover";

    constructor(
        public service: Service,
        private websocket: Websocket,
        public modalCtrl: ModalController,
        private route: ActivatedRoute,
    ) { }

    ngOnInit() {
    }

    ngOnDestroy() {
    }
}