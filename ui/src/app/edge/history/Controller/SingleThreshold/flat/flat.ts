import { Component } from '@angular/core';
import { AbstractFlatWidget } from 'src/app/shared/genericComponents/flat/abstract-flat-widget';
import { ChannelAddress, CurrentData } from 'src/app/shared/shared';

@Component({
    selector: 'singlethresholdWidget',
    templateUrl: './flat.html'
})
export class FlatComponent extends AbstractFlatWidget {

    protected activeSecondsOverPeriod: number | null = null;
    private outputChannelAddress: string | null = null;

    protected override getChannelAddresses(): ChannelAddress[] {
        this.outputChannelAddress = this.config.getComponentProperties(this.componentId)['outputChannelAddress'];
        return []
    }

    protected override onCurrentData(currentData: CurrentData): void {

        // console.log("🚀 ~ file: flat.ts:17 ~ FlatComponent ~ overridegetChannelAddresses ~ this.outputChannelAddress:", this.outputChannelAddress)

        // for (this.outputChannelAddress.valueOf(0)) {

        // }
        this.activeSecondsOverPeriod = this.calculateActiveTimeOverPeriod(currentData.allComponents[this.outputChannelAddress]);
    }

    private calculateActiveTimeOverPeriod(number: number): number {
        console.log("🚀 ~ file: flat.ts:30 ~ FlatComponent ~ calculateActiveTimeOverPeriod ~ number:", number)
        return 0;
    }
}

