// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { ChannelAddress, CurrentData, Utils } from "src/app/shared/shared";

@Component({
    selector: "selfconsumptionWidget",
    templateUrl: "./flat.html",
})
export class FlatComponent extends AbstractFlatWidget {

    protected selfconsumptionValue: number | null;

    protected override onCurrentData(currentData: CurrentData) {
        this.selfconsumptionValue = Utils.calculateSelfConsumption(
            currentData.allComponents["_sum/GridSellActiveEnergy"],
            currentData.allComponents["_sum/ProductionActiveEnergy"],
        );
    }

    protected override getChannelAddresses(): ChannelAddress[] {
        return [
            new ChannelAddress("_sum", "GridSellActiveEnergy"),
            new ChannelAddress("_sum", "ProductionActiveEnergy"),
        ];
    }
}
