// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { ChannelAddress, CurrentData, Utils } from "../../../../../shared/shared";

@Component({
    selector: "autarchyWidget",
    templateUrl: "./FLAT.HTML",
    standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {

    protected autarchyValue: number | null;

    protected override onCurrentData(currentData: CurrentData) {
        THIS.AUTARCHY_VALUE =
            UTILS.CALCULATE_AUTARCHY(
                CURRENT_DATA.ALL_COMPONENTS["_sum/GridBuyActiveEnergy"] / 1000,
                CURRENT_DATA.ALL_COMPONENTS["_sum/ConsumptionActiveEnergy"] / 1000);
    }

    protected override getChannelAddresses(): ChannelAddress[] {
        return [
            new ChannelAddress("_sum", "GridBuyActiveEnergy"),
            new ChannelAddress("_sum", "ConsumptionActiveEnergy"),
        ];
    }
}

