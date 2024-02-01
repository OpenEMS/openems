import { Component, Input } from '@angular/core';

import { AbstractFlatWidget } from 'src/app/shared/genericComponents/flat/abstract-flat-widget';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChannelAddress, CurrentData } from 'src/app/shared/shared';

@Component({
    selector: 'timeOfUseTariffWidget',
    templateUrl: './flat.html',
})
export class FlatComponent extends AbstractFlatWidget {

    @Input() public period: DefaultTypes.HistoryPeriod;

    protected delayedActiveTimeOverPeriod: number | null = null;
    protected chargedActiveTimeOverPeriod: number | null = null;

    protected override onCurrentData(currentData: CurrentData) {
        this.delayedActiveTimeOverPeriod = currentData.allComponents[this.componentId + '/DelayedTime'];
        this.chargedActiveTimeOverPeriod = currentData.allComponents[this.componentId + '/ChargedTime'];
    }

    override getChannelAddresses(): ChannelAddress[] {
        return [
            new ChannelAddress(this.componentId, 'DelayedTime'),
            new ChannelAddress(this.componentId, 'ChargedTime'),
        ];
    }
}
