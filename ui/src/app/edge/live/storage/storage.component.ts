import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, Service, Websocket, EdgeConfig } from '../../../shared/shared';
import { ModalController } from '@ionic/angular';
import { StorageModalComponent } from './modal/modal.component';

@Component({
    selector: 'storage',
    templateUrl: './storage.component.html'
})
export class StorageComponent {

    private static readonly SELECTOR = "storage";

    public edge: Edge = null;


    constructor(
        public service: Service,
        private websocket: Websocket,
        private route: ActivatedRoute,
        public modalController: ModalController,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
            edge.subscribeChannels(this.websocket, StorageComponent.SELECTOR, [
                // Ess
                new ChannelAddress('_sum', 'EssSoc'),
                new ChannelAddress('_sum', 'EssActivePower'),
            ]);
        });
    }

    ngOnDestroy() {
        if (this.edge != null) {
            this.edge.unsubscribeChannels(this.websocket, StorageComponent.SELECTOR);
        }
    }

    async presentModal() {
        const modal = await this.modalController.create({
            component: StorageModalComponent,
            componentProps: {
                edge: this.edge
            }
        });
        return await modal.present();
    }

}
