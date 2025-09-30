import { Component, Input } from "@angular/core";

import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { ChannelAddress, CurrentData } from "src/app/shared/shared";
import { DefaultTypes } from "src/app/shared/type/defaulttypes";

@Component({
    selector: "timeOfUseTariffWidget",
    templateUrl: "./FLAT.HTML",
    standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {

    @Input({ required: true }) public period!: DEFAULT_TYPES.HISTORY_PERIOD;

    protected delayedActiveTimeOverPeriod: number | null = null;
    protected chargedConsumptionActiveTimeOverPeriod: number | null = null;

    override getChannelAddresses(): ChannelAddress[] {
        return [
            new ChannelAddress(THIS.COMPONENT_ID, "DelayedTime"),
            new ChannelAddress(THIS.COMPONENT_ID, "ChargedTime"),
        ];
    }

    protected override onCurrentData(currentData: CurrentData) {
        THIS.DELAYED_ACTIVE_TIME_OVER_PERIOD = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT_ID + "/DelayedTime"];
        THIS.CHARGED_CONSUMPTION_ACTIVE_TIME_OVER_PERIOD = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT_ID + "/ChargedTime"];
    }
}
