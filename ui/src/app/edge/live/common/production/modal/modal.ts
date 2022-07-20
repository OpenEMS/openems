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

    public productionMeterComponents: EdgeConfig.Component[] = [];
    public chargerComponents: EdgeConfig.Component[] = [];
    public arePhasesNotNull: boolean = false;

    protected override getChannelAddresses() {

        // Get Chargers
        this.chargerComponents =
            this.config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger")
                .filter(component => component.isEnabled);

        // Get productionMeters
        this.productionMeterComponents =
            this.config.getComponentsImplementingNature("io.openems.edge.meter.api.SymmetricMeter")
                .filter(component => component.isEnabled
                    && this.config.isProducer(component));

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