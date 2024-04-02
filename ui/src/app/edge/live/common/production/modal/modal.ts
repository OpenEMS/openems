import { Component } from '@angular/core';
import { AbstractModal } from 'src/app/shared/genericComponents/modal/abstractModal';
import { ChannelAddress, EdgeConfig, Utils } from 'src/app/shared/shared';

@Component({
    templateUrl: './modal.html',
})
export class ModalComponent extends AbstractModal {

    // reference to the Utils method to access via html
    public readonly isLastElement = Utils.isLastElement;
    public readonly CONVERT_TO_WATT = Utils.CONVERT_TO_WATT;

    public productionMeters: EdgeConfig.Component[] = [];
    public chargerComponents: EdgeConfig.Component[] = [];
    public isAsymmetric: boolean = false;

    protected override getChannelAddresses() {
        const channelAddresses: ChannelAddress[] = [];

        // Get Chargers
        this.chargerComponents =
            this.config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger")
                .filter(component => component.isEnabled);

        // Get productionMeters
        this.config.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
            .filter(component => component.isEnabled && this.config.isProducer(component))
            .forEach(component => {
                channelAddresses.push(new ChannelAddress(component.id, 'ActivePower'));
                channelAddresses.push(new ChannelAddress(component.id, 'ActivePowerL1'));
                channelAddresses.push(new ChannelAddress(component.id, 'ActivePowerL2'));
                channelAddresses.push(new ChannelAddress(component.id, 'ActivePowerL3'));
                this.productionMeters.push(component);
            });

        return channelAddresses;
    }
}
