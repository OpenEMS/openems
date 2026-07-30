// @ts-strict-ignore
import { Component, ChangeDetectionStrategy } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";

import { Modal } from "src/app/shared/components/flat/flat";
import { CurrentData, Utils } from "../../../../../shared/shared";
import { SharedControllerPeakShaving } from "../shared/shared";
import { Controller_Symmetric_TimeSlot_PeakShavingModalComponent } from "./modal/modal.component";

@Component({
    selector: "Controller_Symmetric_TimeSlot_PeakShaving",
    templateUrl: "./Symmetric_TimeSlot.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class Controller_Symmetric_TimeSlot_PeakShavingComponent extends AbstractFlatWidget {
    public activePower: number;
    public peakShavingPower: number;
    public rechargePower: number;
    public readonly CONVERT_WATT_TO_KILOWATT = Utils.CONVERT_WATT_TO_KILOWATT;
    protected modalComponent: Modal | null = null;
    protected override afterIsInitialized(): void {
        this.modalComponent = this.getModalComponent();
    }
    protected getModalComponent(): Modal {
        return {
            component: Controller_Symmetric_TimeSlot_PeakShavingModalComponent,
            componentProps: {
                component: this.component,
                edge: this.edge,
            },
        };
    }

    protected override getChannelAddresses() {
        return SharedControllerPeakShaving.getWidgetChannelAddresses(
            this.component.getPropertyFromComponent<string>("meter.id"),
            this.componentId,
        );
    }
    protected override onCurrentData(currentData: CurrentData) {
        const values = SharedControllerPeakShaving.getWidgetValues(
            currentData,
            this.component.getPropertyFromComponent<string>("meter.id"),
            this.component.getPropertyFromComponent<number>("peakShavingPower") ?? 0,
            this.component.getPropertyFromComponent<number>("rechargePower") ?? 0,
        );

        this.activePower = values.activePower;
        this.peakShavingPower = values.peakShavingPower;
        this.rechargePower = values.rechargePower;
    }
}
