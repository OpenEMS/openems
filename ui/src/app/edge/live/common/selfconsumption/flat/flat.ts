// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { ChannelAddress, CurrentData, Utils } from "src/app/shared/shared";

import { ModalComponent } from "../modal/modal";

@Component({
    selector: "Common_Selfconsumption",
    templateUrl: "./flat.html",
})
export class FlatComponent extends AbstractFlatWidget {

    public calculatedSelfConsumption: number;

    async presentModal() {
        const modal = await this.modalController.create({
            component: ModalComponent,
        });
        return await modal.present();
    }

    protected override getChannelAddresses() {
        return [
            new ChannelAddress("_sum", "GridActivePower"),
            new ChannelAddress("_sum", "ProductionActivePower"),
        ];
    }

    protected override onCurrentData(currentData: CurrentData) {
        this.calculatedSelfConsumption = Utils.calculateSelfConsumption(
            Utils.multiplySafely(
                currentData.allComponents["_sum/GridActivePower"],
                -1,
            ),
            currentData.allComponents["_sum/ProductionActivePower"],
        );
    }

}
