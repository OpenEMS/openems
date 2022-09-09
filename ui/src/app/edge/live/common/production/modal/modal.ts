import { Component } from '@angular/core';
import { ChannelAddress, CurrentData, EdgeConfig, Utils } from 'src/app/shared/shared';
import { AbstractModal } from 'src/app/shared/genericComponents/modal/abstractModal';

@Component({
    templateUrl: './modal.html'
})
export class Modal extends AbstractModal {

    // reference to the Utils method to access via html
    public readonly isLastElement = Utils.isLastElement;
    public readonly CONVERT_TO_WATT = Utils.CONVERT_TO_WATT;

    public productionMeters: { component: EdgeConfig.Component, isAsymmetric: boolean }[] = [];
    public chargerComponents: EdgeConfig.Component[] = [];
    public arePhasesNotNull: boolean = false;
    public isAsymmetric: boolean = false;

    protected override getChannelAddresses() {

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
                this.productionMeters.push({ component: component, isAsymmetric: asymmetricMeters.filter(element => component.id == element.id).length > 0 })
            })

        return [
            new ChannelAddress('_sum', 'ProductionAcActivePowerL1'),
            new ChannelAddress('_sum', 'ProductionAcActivePowerL2'),
            new ChannelAddress('_sum', 'ProductionAcActivePowerL3'),
        ]
    }

    protected override onCurrentData(currentData: CurrentData) {

        let activePower = {};
        for (let phase of ['L1', 'L2', 'L3']) {
            activePower[phase] = currentData.allComponents['_sum/ProductionAcActivePower' + phase]
        }

        this.arePhasesNotNull = Object.values(activePower).every(phase => phase != null);
    }
}