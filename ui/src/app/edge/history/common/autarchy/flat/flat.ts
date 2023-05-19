import { ChannelAddress, CurrentData, Utils } from '../../../../../shared/shared';
import { Component } from '@angular/core';
import { AbstractHistoryWidget } from 'src/app/shared/genericComponents/abstracthistorywidget';

@Component({
    selector: 'autarchyWidget',
    templateUrl: './flat.html'
})
export class FlatComponent extends AbstractHistoryWidget {

    public autarchyValue: number;

    protected override onCurrentData(currentData: CurrentData) {
        this.autarchyValue =
            Utils.calculateAutarchy(
                currentData.allComponents['_sum/GridBuyActiveEnergy'] / 1000,
                currentData.allComponents['_sum/ConsumptionActiveEnergy'] / 1000);
    }

    protected override getChannelAddresses(): ChannelAddress[] {
        return [
            new ChannelAddress('_sum', 'GridBuyActiveEnergy'),
            new ChannelAddress('_sum', 'ConsumptionActiveEnergy'),
        ];
    }
}

