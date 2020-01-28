import { Component } from '@angular/core';

import { Edge, EdgeConfig, Service, Websocket, ChannelAddress } from '../../../shared/shared';

import { ActivatedRoute } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { KacoErrorModalComponent } from './modal/modal.component';




@Component({
    selector: 'kacoerror',
    templateUrl: './kacoerror.component.html'
})

export class KacoErrorComponent {
    private static readonly SELECTOR = "kacoerror";

    public edge: Edge = null;
    public config: EdgeConfig = null;
    public essComponents: EdgeConfig.Component[] = null;

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

            this.essComponents = config.getComponentsImplementingNature("io.openems.edge.ess.api.SymmetricEss").filter(component => !component.factoryId.includes("Ess.Cluster") && component.isEnabled);
            for (let component of this.essComponents) {


                channels.push(
                    new ChannelAddress(component.id, 'ErrorLog'),
                    new ChannelAddress(component.id, 'ErrorList')
                );
            }
            this.edge.subscribeChannels(this.websocket, KacoErrorComponent.SELECTOR, channels);
        })
    }

    ngOnDestroy() {
        if (this.edge != null) {
            this.edge.unsubscribeChannels(this.websocket, KacoErrorComponent.SELECTOR);
        }
    }

    async presentModal() {
        const modal = await this.modalCtrl.create({
            component: KacoErrorModalComponent,
            componentProps: {
                edge: this.edge,
                config: this.config,
                essComponents: this.essComponents,
            }
        });
        return await modal.present();
    }
}