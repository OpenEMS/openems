import { Component } from '@angular/core';
import { AbstractFlatWidget } from 'src/app/shared/genericComponents/flat/abstract-flat-widget';
import { ChannelAddress } from 'src/app/shared/shared';

@Component({
    selector: 'singlethresholdWidget',
    templateUrl: './flat.html'
})
export class FlatComponent extends AbstractFlatWidget {

    protected outputChannelAddress: string | null = null;

    protected override getChannelAddresses(): ChannelAddress[] {

        // TODO if PR for Cumulated Channels is merged, use channelAddress here
        this.outputChannelAddress = this.config.getComponentProperties(this.componentId)['outputChannelAddress'];
        return []
    }
}

