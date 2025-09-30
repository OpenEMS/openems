// @ts-strict-ignore
import { Component } from "@angular/core";

import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { Converter } from "src/app/shared/components/shared/converter";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";

@Component({
    selector: "channelthresholdWidget",
    templateUrl: "./FLAT.HTML",
    standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {

    protected displayName: Map<string, string> = new Map();

    protected activeSecondsOverPeriod: number | null = null;
    protected FORMAT_SECONDS_TO_DURATION = Converter.FORMAT_SECONDS_TO_DURATION(THIS.TRANSLATE.CURRENT_LANG);

    protected controllers: EDGE_CONFIG.COMPONENT[] | null = [];

    protected override getChannelAddresses(): ChannelAddress[] {

        THIS.CONTROLLERS = THIS.CONFIG.GET_COMPONENTS_BY_FACTORY("CONTROLLER.CHANNEL_THRESHOLD").concat(THIS.CONFIG.GET_COMPONENTS_IMPLEMENTING_NATURE("IO.OPENEMS.IMPL.CONTROLLER.CHANNELTHRESHOLD.CHANNEL_THRESHOLD_CONTROLLER"));

        const channelAddresses: ChannelAddress[] = [];

        for (const controller of THIS.CONTROLLERS) {
            const output: ChannelAddress | null = CHANNEL_ADDRESS.FROM_STRING(CONTROLLER.PROPERTIES["outputChannelAddress"]);
            THIS.DISPLAY_NAME.SET(CONTROLLER.ID, THIS.GET_DISPLAY_NAME(controller, output));
            CHANNEL_ADDRESSES.PUSH(new ChannelAddress(CONTROLLER.ID, "CumulatedActiveTime"));
        }
        return channelAddresses;
    }

    private getDisplayName(controller: EDGE_CONFIG.COMPONENT | null, output: ChannelAddress | null): string {
        return CONTROLLER.ID === CONTROLLER.ALIAS ? OUTPUT.CHANNEL_ID : CONTROLLER.ALIAS;
    }
}
