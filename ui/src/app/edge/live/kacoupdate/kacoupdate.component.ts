import { Component, SimpleChanges } from '@angular/core';

import { Edge, EdgeConfig, Service, Websocket, ChannelAddress } from '../../../shared/shared';

import { ActivatedRoute } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { KacoUpdateModalComponent } from './modal/modal.component';
import { HasUpdateRequest } from 'src/app/shared/jsonrpc/request/hasUpdateRequest';




@Component({
    selector: 'kacoupdate',
    templateUrl: './kacoupdate.component.html'
})

export class KacoUpdateComponent {
    private static readonly SELECTOR = "kacoupdate";

    public edge: Edge = null;
    public config: EdgeConfig = null;

    constructor(
        public service: Service,
        private websocket: Websocket,
        private route: ActivatedRoute,
        public modalCtrl: ModalController,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
        });
        this.service.getConfig().then(config => {
            this.config = config;
            let channels = [];
            channels.push(
                new ChannelAddress('_kacoUpdate', 'HasUpdate'),
                new ChannelAddress('_kacoUpdate', 'HasEdgeUpdate'),
                new ChannelAddress('_kacoUpdate', 'HasUiUpdate'),
            );
            this.edge.subscribeChannels(this.websocket, KacoUpdateComponent.SELECTOR, channels);
            let request = new HasUpdateRequest();
            this.edge.sendRequest(this.websocket, request).then(response => {
                if (Object.keys(response.result).length != 0) {
                    this.presentModal();
                }
            });
        });



    }

    ngOnDestroy() {
        if (this.edge != null) {
            this.edge.unsubscribeChannels(this.websocket, KacoUpdateComponent.SELECTOR);
        }
    }


    async presentModal() {
        const modal = await this.modalCtrl.create({
            component: KacoUpdateModalComponent,
            componentProps: {
                edge: this.edge,
                config: this.config,
            }
        });
        return await modal.present();
    }
}