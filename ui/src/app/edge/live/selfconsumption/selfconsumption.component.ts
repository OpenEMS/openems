import { ActivatedRoute } from '@angular/router';
import { Edge, Service, Websocket } from '../../../shared/shared';
import { Component } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { SelfconsumptionModalComponent } from './modal/modal.component';

@Component({
    selector: 'selfconsumption',
    templateUrl: './selfconsumption.component.html'
})
export class SelfConsumptionComponent {

    private static readonly SELECTOR = "selfconsumption";

    public edge: Edge = null;

    constructor(
        private route: ActivatedRoute,
        private websocket: Websocket,
        public modalCtrl: ModalController,
        public service: Service,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
        });
    }

    async presentModal() {
        const modal = await this.modalCtrl.create({
            component: SelfconsumptionModalComponent,
        });
        return await modal.present();
    }
}
