// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { ChannelAddress, CurrentData, Utils } from "src/app/shared/shared";

import { ModalComponent } from "../modal/modal";

@Component({
    selector: "Common_Selfconsumption",
    templateUrl: "./FLAT.HTML",
    standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {

    public calculatedSelfConsumption: number;

    async presentModal() {
        const modal = await THIS.MODAL_CONTROLLER.CREATE({
            component: ModalComponent,
        });
        return await MODAL.PRESENT();
    }

    protected override getChannelAddresses() {
        return [
            new ChannelAddress("_sum", "GridActivePower"),
            new ChannelAddress("_sum", "ProductionActivePower"),
        ];
    }

    protected override onCurrentData(currentData: CurrentData) {
        THIS.CALCULATED_SELF_CONSUMPTION = UTILS.CALCULATE_SELF_CONSUMPTION(
            UTILS.MULTIPLY_SAFELY(
                CURRENT_DATA.ALL_COMPONENTS["_sum/GridActivePower"],
                -1,
            ),
            CURRENT_DATA.ALL_COMPONENTS["_sum/ProductionActivePower"],
        );
    }

}
