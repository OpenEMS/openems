import { Component, Input } from '@angular/core';

import { AbstractFlatWidget } from 'src/app/shared/components/flat/abstract-flat-widget';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChannelAddress, CurrentData } from 'src/app/shared/shared';

@Component({
    selector: 'timeOfUseTariffWidget',
    templateUrl: './flat.html',
})
export class FlatComponent extends AbstractFlatWidget {

    @Input({ required: true }) public period!: DefaultTypes.HistoryPeriod;

    protected delayedActiveTimeOverPeriod: number | null = null;
    protected chargedConsumptionActiveTimeOverPeriod: number | null = null;

    override getChannelAddresses(): ChannelAddress[] {
        return [
            new ChannelAddress(this.componentId, 'DelayedTime'),
            new ChannelAddress(this.componentId, 'ChargedTime'),
        ];
    }

    protected override onCurrentData(currentData: CurrentData) {
        this.delayedActiveTimeOverPeriod = currentData.allComponents[this.componentId + '/DelayedTime'];
        this.chargedConsumptionActiveTimeOverPeriod = currentData.allComponents[this.componentId + '/ChargedTime'];
    }

}
