import { ChannelAddress, EdgeConfig, Utils } from '../../../shared/shared';
import { Component } from '@angular/core';
import { ProductionModalComponent } from './modal/modal.component';
import { AbstractFlatWidget } from '../flat/abstract-flat-widget';

@Component({
    selector: 'production',
    templateUrl: './production.component.html'
})
export class ProductionComponent extends AbstractFlatWidget {

    public productionMeterComponents: EdgeConfig.Component[] = [];
    public chargerComponents: EdgeConfig.Component[] = [];
    public readonly CONVERT_WATT_TO_KILOWATT = Utils.CONVERT_WATT_TO_KILOWATT;

    protected getChannelAddresses() {
        let channelAddresses: ChannelAddress[] = [
            new ChannelAddress('_sum', 'ProductionActivePower'),
            new ChannelAddress('_sum', 'ProductionAcActivePower'),

            // TODO should be moved to Modal
            new ChannelAddress('_sum', 'ProductionAcActivePowerL1'),
            new ChannelAddress('_sum', 'ProductionAcActivePowerL2'),
            new ChannelAddress('_sum', 'ProductionAcActivePowerL3'),
        ]
        // Get Chargers
        this.chargerComponents = 
        this.config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger")
            .filter(component => component.isEnabled);

        // Get productionMeters
        this.productionMeterComponents = 
        this.config.getComponentsImplementingNature("io.openems.edge.meter.api.SymmetricMeter")
            .filter(component => component.isEnabled && this.config.isProducer(component));

        return channelAddresses
    }

    async presentModal() {
        const modal = await this.modalController.create({
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
