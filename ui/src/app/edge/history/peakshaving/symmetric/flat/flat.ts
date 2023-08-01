import { Component } from '@angular/core';
import { AbstractFlatWidget } from 'src/app/shared/genericComponents/flat/abstract-flat-widget';
import { ChannelAddress, CurrentData, Utils, EdgeConfig, } from '../../../../../shared/shared';

@Component({
    selector: 'symmetricWidget',
    templateUrl: './flat.html'
})
export class FlatComponent extends AbstractFlatWidget {

    protected autarchyValue: number | null;
    public productionMeterComponents: EdgeConfig.Component[] = [];
    protected override onCurrentData(currentData: CurrentData) {

    }

    protected override getChannelAddresses(): ChannelAddress[] {
        this.productionMeterComponents =
            this.config.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
                .filter(component => component.isEnabled && this.config.isProducer(component));
        return [];

    }
}
