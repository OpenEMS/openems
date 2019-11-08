import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { ChannelAddress, Edge, Service, Websocket } from '../../../shared/shared';
import { AwattarModalComponent } from './modal/modal.component';

@Component({
    selector: AwattarComponent.SELECTOR,
    templateUrl: './awattar.component.html'
})
export class AwattarComponent {

    private static readonly SELECTOR = "awattar";

    private edge: Edge = null;

    constructor(
        public service: Service,
        private websocket: Websocket,
        private route: ActivatedRoute,
        public modalCtrl: ModalController,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
            //   edge.subscribeChannels(this.websocket, AutarchyComponent.SELECTOR, [
            //     // Grid
            //     new ChannelAddress('_sum', 'GridActivePower'),
            //     // Consumption
            //     new ChannelAddress('_sum', 'ConsumptionActivePower')
            //   ]);
        });
    }

    ngOnDestroy() {
        if (this.edge != null) {
            this.edge.unsubscribeChannels(this.websocket, AwattarComponent.SELECTOR);
        }
    }

    async presentModal() {
        const modal = await this.modalCtrl.create({
            component: AwattarModalComponent,
            cssClass: 'wide-modal'
        });
        return await modal.present();
    }
}
