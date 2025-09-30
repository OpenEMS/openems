// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { ChannelAddress, CurrentData, Utils } from "src/app/shared/shared";

@Component({
    selector: "selfconsumptionWidget",
    templateUrl: "./FLAT.HTML",
    standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {

    protected selfconsumptionValue: number | null;

    protected override onCurrentData(currentData: CurrentData) {
        THIS.SELFCONSUMPTION_VALUE = UTILS.CALCULATE_SELF_CONSUMPTION(
            CURRENT_DATA.ALL_COMPONENTS["_sum/GridSellActiveEnergy"],
            CURRENT_DATA.ALL_COMPONENTS["_sum/ProductionActiveEnergy"],
        );
    }

    protected override getChannelAddresses(): ChannelAddress[] {
        return [
            new ChannelAddress("_sum", "GridSellActiveEnergy"),
            new ChannelAddress("_sum", "ProductionActiveEnergy"),
        ];
    }
}
