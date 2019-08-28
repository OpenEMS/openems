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
            this.chargerComponents = config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger").filter(component => !component.isEnabled == false);
            for (let component of this.chargerComponents) {
                channels.push(
                    new ChannelAddress(component.id, 'ActualPower'),
                )
            }
            this.productionMeterComponents = config.getComponentsImplementingNature("io.openems.edge.meter.api.SymmetricMeter").filter(component => {
                if (component.isEnabled == false) {
                    return false;
                }
                else if (component.properties['type'] == "PRODUCTION") {
                    return true;
                } else {
                    // TODO make sure 'type' is provided for all Meters
                    switch (component.factoryId) {
                        case 'Fenecon.Mini.PvMeter':
                        case 'Fenecon.Dess.PvMeter':
                        case 'Fenecon.Pro.PvMeter':
                        case 'Kostal.Piko.Charger':
                        case 'Kaco.BlueplanetHybrid10.PvInverter':
                        case 'PV-Inverter.Solarlog':
                        case 'PV-Inverter.KACO.blueplanet':
                        case 'SolarEdge.PV-Inverter':
                        case 'Simulator.ProductionMeter.Acting':
                            return true;
                    }
                }
                return false;
            })
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