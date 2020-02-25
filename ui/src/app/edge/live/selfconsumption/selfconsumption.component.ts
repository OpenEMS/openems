import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, Service, Websocket } from '../../../shared/shared';
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
            edge.subscribeChannels(this.websocket, SelfConsumptionComponent.SELECTOR, [
                // Ess
                new ChannelAddress('_sum', 'EssActivePower'),
                // Grid
                new ChannelAddress('_sum', 'GridActivePower'),
                // Production
                new ChannelAddress('_sum', 'ProductionActivePower'), new ChannelAddress('_sum', 'ProductionDcActualPower'), new ChannelAddress('_sum', 'ProductionAcActivePower')
            ]);
        });
    }

    ngOnDestroy() {
        if (this.edge != null) {
            this.edge.unsubscribeChannels(this.websocket, SelfConsumptionComponent.SELECTOR);
        }
    }

    async presentModal() {
        const modal = await this.modalCtrl.create({
            component: SelfconsumptionModalComponent,
        });
        return await modal.present();
    }
}
