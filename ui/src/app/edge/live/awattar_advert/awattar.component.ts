import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { ChannelAddress, Edge, Service, Websocket } from '../../../shared/shared';
import { AwattarAdvertModalComponent } from './modal/modal.component';

@Component({
    selector: AwattarAdvertComponent.SELECTOR,
    templateUrl: './awattar.component.html'
})
export class AwattarAdvertComponent {

    private static readonly SELECTOR = "awattar-advert";

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
            this.edge.unsubscribeChannels(this.websocket, AwattarAdvertComponent.SELECTOR);
        }
    }

    async presentModal() {
        const modal = await this.modalCtrl.create({
            component: AwattarAdvertModalComponent,
            cssClass: 'wide-modal'
        });
        return await modal.present();
    }
}
