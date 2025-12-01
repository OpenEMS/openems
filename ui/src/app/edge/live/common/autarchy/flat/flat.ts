// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { Modal } from "src/app/shared/components/flat/flat";
import { ChannelAddress, CurrentData, Utils } from "src/app/shared/shared";
import { ModalComponent } from "../modal/modal";

@Component({
    selector: "Common_Autarchy",
    templateUrl: "./flat.html",
    standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {

    public percentageValue: number;
    protected modalComponent: Modal | null = null;

    protected getModalComponent(): Modal {
        return { component: ModalComponent };
    };

    protected override getChannelAddresses(): ChannelAddress[] {
        return [
            new ChannelAddress("_sum", "GridActivePower"),
            new ChannelAddress("_sum", "ConsumptionActivePower"),
        ];
    }

    protected override onCurrentData(currentData: CurrentData) {
        this.percentageValue = Utils.calculateAutarchy(
            currentData.allComponents["_sum/GridActivePower"],
            currentData.allComponents["_sum/ConsumptionActivePower"],
        );
    }

    protected override afterIsInitialized(): void {
        this.modalComponent = this.getModalComponent();
    }

}
