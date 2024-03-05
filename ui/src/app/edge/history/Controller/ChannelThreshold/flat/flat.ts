import { Component } from '@angular/core';

import { AbstractFlatWidget } from 'src/app/shared/genericComponents/flat/abstract-flat-widget';
import { Converter } from 'src/app/shared/genericComponents/shared/converter';
import { ChannelAddress, EdgeConfig } from 'src/app/shared/shared';

@Component({
    selector: 'channelthresholdWidget',
    templateUrl: './flat.html',
})
export class FlatComponent extends AbstractFlatWidget {

    protected displayName: Map<string, string> = new Map();

    protected activeSecondsOverPeriod: number | null = null;
    protected FORMAT_SECONDS_TO_DURATION = Converter.FORMAT_SECONDS_TO_DURATION(this.translate.currentLang);

    protected controllers: EdgeConfig.Component[] | null = [];

    protected override getChannelAddresses(): ChannelAddress[] {

        this.controllers = this.config.getComponentsByFactory('Controller.ChannelThreshold').concat(this.config.getComponentsImplementingNature('io.openems.impl.controller.channelthreshold.ChannelThresholdController'));

        const channelAddresses: ChannelAddress[] = [];

        for (const controller of this.controllers) {
            const output: ChannelAddress | null = ChannelAddress.fromString(controller.properties['outputChannelAddress']);
            this.displayName.set(controller.id, this.getDisplayName(controller, output));
            channelAddresses.push(new ChannelAddress(controller.id, 'CumulatedActiveTime'));
        }
        return channelAddresses;
    }

    private getDisplayName(controller: EdgeConfig.Component | null, output: ChannelAddress | null): string {
        return controller.id === controller.alias ? output.channelId : controller.alias;
    }
}
