import { Component, Input } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, Service, Websocket, EdgeConfig } from '../../../../shared/shared';
import { ModalController } from '@ionic/angular';

@Component({
    selector: 'production-modal',
    templateUrl: './modal.component.html'
})
export class ProductionModalComponent {

    private static readonly SELECTOR = "production-modal";

    @Input() edge: Edge;

    public config: EdgeConfig = null;
    public productionMeterComponents: EdgeConfig.Component[] = null;
    public chargerComponents: EdgeConfig.Component[] = null;


    constructor(
        public service: Service,
        private websocket: Websocket,
        private route: ActivatedRoute,
        public modalCtrl: ModalController,
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
            this.productionMeterComponents = config.getComponentsImplementingNature("io.openems.edge.meter.api.SymmetricMeter").filter(component => component.properties['type'] == "PRODUCTION");
            for (let component of this.productionMeterComponents) {
                let factoryID = component.factoryId;
                let factory = config.factories[factoryID];
                channels.push(
                    new ChannelAddress(component.id, 'ActivePower')
                );
                if ((factory.natureIds.includes("io.openems.edge.meter.api.AsymmetricMeter"))) {
                    channels.push(
                        new ChannelAddress(component.id, 'ActivePowerL1'),
                        new ChannelAddress(component.id, 'ActivePowerL2'),
                        new ChannelAddress(component.id, 'ActivePowerL3')
                    );
                }
            }
            this.edge.subscribeChannels(this.websocket, ProductionModalComponent.SELECTOR, channels);
        })
    }

    ngOnDestroy() {
        if (this.edge != null) {
            this.edge.unsubscribeChannels(this.websocket, ProductionModalComponent.SELECTOR);
        }
    }
}
