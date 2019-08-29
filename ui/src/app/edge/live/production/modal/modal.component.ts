import { Component, Input } from '@angular/core';
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
    //Boolean Value to show Info Text in HTML Component
    public isAsymmetric: Boolean = false;


    constructor(
        public service: Service,
        private websocket: Websocket,
        public modalCtrl: ModalController,
    ) { }

    ngOnInit() {
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
            console.log("PRODUCTIONMETERS", this.productionMeterComponents);
            for (let component of this.productionMeterComponents) {
                let factoryID = component.factoryId;
                let factory = config.factories[factoryID];
                channels.push(
                    new ChannelAddress(component.id, 'ActivePower')
                );
                if ((factory.natureIds.includes("io.openems.edge.meter.api.AsymmetricMeter"))) {
                    this.isAsymmetric = true;
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