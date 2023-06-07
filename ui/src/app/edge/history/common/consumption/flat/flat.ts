import { Component } from '@angular/core';
import { AbstractFlatWidget } from 'src/app/shared/genericComponents/flat/abstract-flat-widget';

import { ChannelAddress, CurrentData, EdgeConfig } from '../../../../../shared/shared';

@Component({
    selector: 'consumptionWidget',
    templateUrl: './flat.html'
})
export class FlatComponent extends AbstractFlatWidget {

    public evcsComponents: EdgeConfig.Component[] = [];
    public consumptionMeterComponents: EdgeConfig.Component[] = [];
    totalOtherEnergy: number;

    protected override getChannelAddresses(): ChannelAddress[] {

        this.evcsComponents = this.config.getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs")
            .filter(component =>
                !(component.factoryId == 'Evcs.Cluster.SelfConsumption') &&
                !(component.factoryId == 'Evcs.Cluster.PeakShaving') &&
                !component.isEnabled == false);

        this.consumptionMeterComponents = this.config.getComponentsImplementingNature("io.openems.edge.meter.api.SymmetricMeter")
            .filter(component => component.isEnabled && this.config.isTypeConsumptionMetered(component));

        return [new ChannelAddress('_sum', 'ConsumptionActiveEnergy')];
    }

    protected override onCurrentData(currentData: CurrentData): void {
        this.totalOtherEnergy = this.getTotalOtherEnergy(currentData);
    }

    /**
     * Gets the totalOtherEnergy
     * 
     * @param currentData the currentData
     * @returns the total other Energy
     */
    private getTotalOtherEnergy(currentData: CurrentData): number {
        let otherEnergy: number = 0;
        this.evcsComponents.forEach(component => {
            otherEnergy += currentData.allComponents[component.id + '/ActiveConsumptionEnergy'] ?? 0;
        });
        this.consumptionMeterComponents.forEach(component => {
            otherEnergy += currentData.allComponents[component.id + '/ActiveConsumptionEnergy'] ?? 0;
        });
        return currentData.allComponents["_sum/ConsumptionActiveEnergy"] - otherEnergy;
    }
}

