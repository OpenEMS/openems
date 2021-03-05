import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, Service, Websocket, EdgeConfig } from '../../../shared/shared';
import { Component } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { StorageModalComponent } from './modal/modal.component';

@Component({
    selector: 'storage',
    templateUrl: './storage.component.html'
})
export class StorageComponent {

    private static readonly SELECTOR = "storage";

    public storageItem: string = '<img *ngIf="sum.soc < 20; else twoBars" src="assets/img/storage_20.png" /><ng-template #twoBars><img *ngIf="sum.soc < 40; else threeBars" src="assets/img/storage_40.png" /></ng-template><ng-template #threeBars><img *ngIf="sum.soc < 60; else fourBars" src="assets/img/storage_60.png" /></ng-template><ng-template #fourBars><img *ngIf="sum.soc < 80; else fiveBars" src="assets/img/storage_80.png" /></ng-template><ng-template #fiveBars><img src="assets/img/storage_100.png" /></ng-template>'

    public edge: Edge = null;
    public config: EdgeConfig = null;
    public essComponents: EdgeConfig.Component[] = null;
    public chargerComponents: EdgeConfig.Component[] = null;
    public channelAdresses: ChannelAddress[] = [];

    constructor(
        public service: Service,
        private websocket: Websocket,
        private route: ActivatedRoute,
        public modalCtrl: ModalController,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
            this.service.getConfig().then(config => {
                this.config = config;

                this.chargerComponents = config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger").filter(component => component.isEnabled);
                for (let component of this.chargerComponents) {
                    this.channelAdresses.push(
                        new ChannelAddress(component.id, 'ActualPower'),
                    )
                }
                this.essComponents = config.getComponentsImplementingNature("io.openems.edge.ess.api.SymmetricEss").filter(component => !component.factoryId.includes("Ess.Cluster") && component.isEnabled);
                for (let component of this.essComponents) {
                    let factoryID = component.factoryId;
                    let factory = config.factories[factoryID];
                    this.channelAdresses.push(
                        new ChannelAddress(component.id, 'Soc'),
                        new ChannelAddress(component.id, 'ActivePower'),
                        new ChannelAddress(component.id, 'Capacity'),
                    );
                    if ((factory.natureIds.includes("io.openems.edge.ess.api.AsymmetricEss"))) {
                        // channels for modal component, subscribe here for better UX
                        this.channelAdresses.push(
                            new ChannelAddress(component.id, 'ActivePowerL1'),
                            new ChannelAddress(component.id, 'ActivePowerL2'),
                            new ChannelAddress(component.id, 'ActivePowerL3')
                        );
                        console.log("Channels: ", this.channelAdresses)
                    }
                }
                this.channelAdresses.push(
                    new ChannelAddress('_sum', 'EssSoc'),
                    new ChannelAddress('_sum', 'EssActivePower'),
                    // channels for modal component, subscribe here for better UX
                    new ChannelAddress('_sum', 'EssActivePowerL1'),
                    new ChannelAddress('_sum', 'EssActivePowerL2'),
                    new ChannelAddress('_sum', 'EssActivePowerL3'),
                    new ChannelAddress('_sum', 'EssCapacity'),
                )
                console.log("channels later: ", this.channelAdresses);
                // this.edge.subscribeChannels(this.websocket, StorageComponent.SELECTOR, channelAdress);

                console.log("Fucking channeladress", this.channelAdresses)
                return this.channelAdresses;
                console.log("Fucking edge: ", edge)
            })
        });
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