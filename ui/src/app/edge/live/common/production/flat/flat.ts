import { EdgeConfig, Utils } from 'src/app/shared/shared';
import { Component } from '@angular/core';
import { AbstractFlatWidget } from 'src/app/shared/components/flat/abstract-flat-widget';
import { ModalComponent } from '../modal/modal';

@Component({
    selector: 'Common_Production',
    templateUrl: './flat.html',
})
export class FlatComponent extends AbstractFlatWidget {

    public productionMeterComponents: EdgeConfig.Component[] = [];
    public chargerComponents: EdgeConfig.Component[] = [];
    public readonly CONVERT_WATT_TO_KILOWATT = Utils.CONVERT_WATT_TO_KILOWATT;

    async presentModal() {
        const modal = await this.modalController.create({
            component: ModalComponent,
        });
        return await modal.present();
    }

    protected override getChannelAddresses() {
        // Get Chargers
        this.chargerComponents =
            this.config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger")
                .filter(component => component.isEnabled);

        // Get productionMeters
        this.productionMeterComponents =
            this.config.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
                .filter(component => component.isEnabled && this.config.isProducer(component));

        return [];
    }

}
