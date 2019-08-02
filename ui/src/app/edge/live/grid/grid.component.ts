import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, Service, Websocket } from '../../../shared/shared';
import { GridModalComponent } from './modal/modal.component';
import { ModalController } from '@ionic/angular';

@Component({
    selector: 'grid',
    templateUrl: './grid.component.html'
})
export class GridComponent {

    private static readonly SELECTOR = "grid";

    public edge: Edge = null;

    constructor(
        public service: Service,
        private websocket: Websocket,
        private route: ActivatedRoute,
        public modalCtrl: ModalController,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
            edge.subscribeChannels(this.websocket, GridComponent.SELECTOR, [
                // Grid
                new ChannelAddress('_sum', 'GridActivePower'),
            ]);
        });
    }

    ngOnDestroy() {
        if (this.edge != null) {
            this.edge.unsubscribeChannels(this.websocket, GridComponent.SELECTOR);
        }
    }

    async presentModal() {
        const modal = await this.modalCtrl.create({
            component: GridModalComponent,
            componentProps: {
                edge: this.edge
            }
        });
        return await modal.present();
    }
}
