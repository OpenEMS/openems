// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { Modal } from "src/app/shared/components/flat/flat";
import { ChannelAddress, CurrentData, Utils } from "src/app/shared/shared";

import { ModalComponent } from "../modal/modal";

@Component({
    selector: "Common_Selfconsumption",
    templateUrl: "./flat.html",
    standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {

    public calculatedSelfConsumption: number;
    protected modalComponent: Modal | null = null;

    protected override afterIsInitialized(): void {
        this.modalComponent = this.getModalComponent();
    }
    protected getModalComponent(): Modal {
        return { component: ModalComponent };
    };

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
