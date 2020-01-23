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
    public config: EdgeConfig = null;
    public essComponents: EdgeConfig.Component[] = null;
    public chargerComponents: EdgeConfig.Component[] = null;

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
            this.chargerComponents = config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger").filter(component => component.isEnabled);
            for (let component of this.chargerComponents) {
                channels.push(
                    new ChannelAddress(component.id, 'ActualPower'),
                )
            }
            this.essComponents = config.getComponentsImplementingNature("io.openems.edge.ess.api.SymmetricEss").filter(component => !component.factoryId.includes("Ess.Cluster") && component.isEnabled);
            for (let component of this.essComponents) {
                let factoryID = component.factoryId;
                let factory = config.factories[factoryID];
                channels.push(
                    new ChannelAddress(component.id, 'Soc'),
                    new ChannelAddress(component.id, 'ActivePower'),
                    new ChannelAddress(component.id, 'Capacity'),
                    new ChannelAddress(component.id, 'ErrorLog')
                );
                if ((factory.natureIds.includes("io.openems.edge.ess.api.AsymmetricEss"))) {
                    channels.push(
                        new ChannelAddress(component.id, 'ActivePowerL1'),
                        new ChannelAddress(component.id, 'ActivePowerL2'),
                        new ChannelAddress(component.id, 'ActivePowerL3')
                    );
                }
            }
            channels.push(
                new ChannelAddress('_sum', 'EssSoc'),
                new ChannelAddress('_sum', 'EssActivePower'),
                new ChannelAddress('_sum', 'EssActivePowerL1'),
                new ChannelAddress('_sum', 'EssActivePowerL2'),
                new ChannelAddress('_sum', 'EssActivePowerL3'),
                new ChannelAddress('_sum', 'EssCapacity'),
            )
            this.edge.subscribeChannels(this.websocket, StorageComponent.SELECTOR, channels);
        })
    }

    ngOnDestroy() {
        if (this.edge != null) {
            this.edge.unsubscribeChannels(this.websocket, StorageComponent.SELECTOR);
        }
    }

    async presentModal() {
        const modal = await this.modalCtrl.create({
            component: StorageModalComponent,
            componentProps: {
                edge: this.edge,
                config: this.config,
                essComponents: this.essComponents,
                chargerComponents: this.chargerComponents,
            }
        });
        return await modal.present();
    }
}