import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, Service, Websocket, EdgeConfig } from '../../../shared/shared';
import { Component } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { StorageModalComponent } from './modal/modal.component';
import { CurrentData } from 'src/app/shared/edge/currentdata';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

@Component({
    selector: 'storage',
    templateUrl: './storage.component.html'
})
export class StorageComponent {

    private static readonly SELECTOR = "storage";

    // public storageItem: string;
    // '<img *ngIf="CurrentData.summary.storage.soc < 20; else twoBars" src="assets/img/storage_20.png" /><ng-template #twoBars><img *ngIf="CurrentData.summary.storage.soc < 40; else threeBars" src="assets/img/storage_40.png" /></ng-template><ng-template #threeBars><img *ngIf="CurrentData.summary.storage.soc < 60; else fourBars" src="assets/img/storage_60.png" /></ng-template><ng-template #fourBars><img *ngIf="CurrentData.summary.storage.soc < 80; else fiveBars" src="assets/img/storage_80.png" /></ng-template><ng-template #fiveBars><img src="assets/img/storage_100.png" /></ng-template>'

    public edge: Edge = null;
    public config: EdgeConfig = null;
    public essComponents: EdgeConfig.Component[] = null;
    public chargerComponents: EdgeConfig.Component[] = null;
    public channelAdresses: ChannelAddress[] = [];
    public storageItem: string;
    private stopOnDestroy: Subject<void> = new Subject<void>();

    constructor(
        public service: Service,
        private websocket: Websocket,
        private route: ActivatedRoute,
        public modalCtrl: ModalController,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
            let callFunction: any;
            this.edge.currentData.pipe(takeUntil(this.stopOnDestroy)).subscribe(currentData => {
                let soc = currentData.channel['_sum' + '/EssSoc']
                if (soc <= 20) {
                    this.storageItem = '<img src="assets/img/storage_20.png"/>'
                } else if (soc <= 40 && soc > 20) {
                    this.storageItem = '<img src="assets/img/storage_40.png"/>'
                } else if (soc <= 60 && soc > 40) {
                    this.storageItem = '<img src="assets/img/storage_60.png"/>'
                } else if (soc <= 80 && soc > 60) {
                    this.storageItem = '<img src="assets/img/storage_80.png"/>'
                } else if (soc <= 100 && soc > 80) {
                    this.storageItem = '<img src="assets/img/storage_100.png"/>'
                }
            })
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
                this.edge.subscribeChannels(this.websocket, StorageComponent.SELECTOR, this.channelAdresses);
                return this.channelAdresses;

            })
        });
    }

    ngOnDestroy() {
        if (this.edge != null) {
            this.edge.unsubscribeChannels(this.websocket, StorageComponent.SELECTOR);
        }
        this.stopOnDestroy.next();
        this.stopOnDestroy.complete();
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
