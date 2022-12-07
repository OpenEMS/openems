import { Component } from '@angular/core';
import { AbstractModal } from 'src/app/shared/genericComponents/modal/abstractModal';
import { ChannelAddress, CurrentData, EdgeConfig, Utils } from 'src/app/shared/shared';

@Component({
    templateUrl: './modal.html'
})
export class ModalComponent extends AbstractModal {

    // reference to the Utils method to access via html
    public readonly isLastElement = Utils.isLastElement;
    public readonly CONVERT_TO_WATT = Utils.CONVERT_TO_WATT;

    public productionMeters: { component: EdgeConfig.Component, isAsymmetric: boolean }[] = [];
    public chargerComponents: EdgeConfig.Component[] = [];
    public isAsymmetric: boolean = false;

    protected override getChannelAddresses() {
        let channelAddresses: ChannelAddress[] = [];

        // Get Chargers
        this.chargerComponents =
            this.config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger")
                .filter(component => component.isEnabled);

        const asymmetricMeters = this.config.getComponentsImplementingNature("io.openems.edge.meter.api.AsymmetricMeter")
            .filter(component => component.isEnabled
                && this.config.isProducer(component));

        // Get productionMeters
        this.config.getComponentsImplementingNature("io.openems.edge.meter.api.SymmetricMeter")
            .filter(component => component.isEnabled && this.config.isProducer(component))
            .forEach(component => {
                var isAsymmetric = asymmetricMeters.filter(element => component.id == element.id).length > 0;
                channelAddresses.push(new ChannelAddress(component.id, 'ActivePower'));
                if (isAsymmetric) {
                    ['L1', 'L2', 'L3'].forEach(phase => {
                        channelAddresses.push(new ChannelAddress(component.id, 'ActivePower' + phase));
                    });
                }
                this.productionMeters.push({ component: component, isAsymmetric: isAsymmetric });
            })

        return channelAddresses;
    }
}