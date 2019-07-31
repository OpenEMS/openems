import { Component, OnInit, Input } from '@angular/core';
import { Service, EdgeConfig, Edge, Websocket, ChannelAddress } from 'src/app/shared/shared';
import { TranslateService } from '@ngx-translate/core';
import { ModalController } from '@ionic/angular';

@Component({
    selector: 'storage-modal',
    templateUrl: './modal.component.html',
})
export class StorageModalComponent implements OnInit {

    private static readonly SELECTOR = "storage-modal";

    @Input() edge: Edge;

    public config: EdgeConfig = null;
    public essComponents: EdgeConfig.Component[] = null;
    public chargerComponents: EdgeConfig.Component[] = null;
    public outputChannel: ChannelAddress[] = null;
    //Boolean Value to show Info Text in HTML Component
    public isAsymmetric: Boolean = false;

    constructor(
        public service: Service,
        public translate: TranslateService,
        public modalCtrl: ModalController,
        public websocket: Websocket,
    ) { }

    ngOnInit() {
        this.service.getConfig().then(config => {
            this.config = config;
            let channels = [];
            this.chargerComponents = config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger");
            for (let component of this.chargerComponents) {
                channels.push(
                    new ChannelAddress(component.id, 'ActualPower'),
                )
            }
            this.essComponents = config.getComponentsImplementingNature("io.openems.edge.ess.api.SymmetricEss").filter(component => !component.factoryId.includes("Ess.Cluster"));
            for (let component of this.essComponents) {
                let factoryID = component.factoryId;
                let factory = config.factories[factoryID];
                channels.push(
                    new ChannelAddress(component.id, 'Soc'),
                    new ChannelAddress(component.id, 'ActivePower'),
                    new ChannelAddress(component.id, 'Capacity'),
                );
                if ((factory.natureIds.includes("io.openems.edge.ess.api.AsymmetricEss"))) {
                    this.isAsymmetric = true;
                    channels.push(
                        new ChannelAddress(component.id, 'ActivePowerL1'),
                        new ChannelAddress(component.id, 'ActivePowerL2'),
                        new ChannelAddress(component.id, 'ActivePowerL3')
                    );
                }
            }
            this.edge.subscribeChannels(this.websocket, StorageModalComponent.SELECTOR, channels);
        })
    }

    ngOnDestroy() {
        if (this.edge != null) {
            this.edge.unsubscribeChannels(this.websocket, StorageModalComponent.SELECTOR);
        }
    }
}