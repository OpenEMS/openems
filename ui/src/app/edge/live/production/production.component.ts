import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, Service, Websocket, EdgeConfig } from '../../../shared/shared';
import { Component } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { ProductionModalComponent } from './modal/modal.component';

@Component({
    selector: 'production',
    templateUrl: './production.component.html'
})
export class ProductionComponent {

    private static readonly SELECTOR = "production";

    public config: EdgeConfig = null;
    public edge: Edge = null;
    public productionMeterComponents: EdgeConfig.Component[] = [];
    public chargerComponents: EdgeConfig.Component[] = [];

    constructor(
        private route: ActivatedRoute,
        private websocket: Websocket,
        public modalCtrl: ModalController,
        public service: Service,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
            this.service.getConfig().then(config => {
                this.config = config;
                let channels = [];
                this.chargerComponents = config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger").filter(component => component.isEnabled);
                for (let component of this.chargerComponents) {
                    channels.push(
                        new ChannelAddress(component.id, 'ActualPower'),
                    )
                }
                this.productionMeterComponents = config.getComponentsImplementingNature("io.openems.edge.meter.api.SymmetricMeter").filter(component => component.isEnabled && config.isProducer(component));
                for (let component of this.productionMeterComponents) {
                    channels.push(
                        new ChannelAddress(component.id, 'ActivePower')
                    );
                }
                channels.push(
                    new ChannelAddress('_sum', 'ProductionActivePower'),
                    new ChannelAddress('_sum', 'ProductionAcActivePower'),
                    // channels for modal component, subscribe here for better UX
                    new ChannelAddress('_sum', 'ProductionAcActivePowerL1'),
                    new ChannelAddress('_sum', 'ProductionAcActivePowerL2'),
                    new ChannelAddress('_sum', 'ProductionAcActivePowerL3'),
                )
                this.edge.subscribeChannels(this.websocket, ProductionComponent.SELECTOR, channels);
            })
        })
    };

    ngOnDestroy() {
        if (this.edge != null) {
            this.edge.unsubscribeChannels(this.websocket, ProductionComponent.SELECTOR);
        }
    }

    async presentModal() {
        const modal = await this.modalCtrl.create({
            component: ProductionModalComponent,
            componentProps: {
                edge: this.edge,
                chargerComponents: this.chargerComponents,
                productionMeterComponents: this.productionMeterComponents,
                config: this.config
            }
        });
        return await modal.present();
    }
}
