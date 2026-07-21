// @ts-strict-ignore
import { Component, ChangeDetectionStrategy } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";

import { Modal } from "src/app/shared/components/flat/flat";
import { Converter } from "src/app/shared/components/shared/converter";
import { CurrentData } from "../../../../../shared/shared";
import { SharedControllerPeakShaving } from "../shared/shared";
import { Controller_Symmetric_PeakShavingModalComponent } from "./modal/modal.component";

@Component({
    selector: "Controller_Symmetric_PeakShaving",
    templateUrl: "./Symmetric.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class Controller_Symmetric_PeakShavingComponent extends AbstractFlatWidget {
    public activePower: number;
    public peakShavingPower: number;
    public rechargePower: number;
    public readonly CONVERT_WATT_TO_KILOWATT = Converter.POWER_IN_KILO_WATT;

    protected modalComponent: Modal | null = null;
    protected override afterIsInitialized(): void {
        this.modalComponent = this.getModalComponent();
    }
    protected getModalComponent(): Modal {
        return {
            component: Controller_Symmetric_PeakShavingModalComponent,
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
            this.component.getPropertyFromComponent<number>("peakShavingPower"),
            this.component.getPropertyFromComponent<number>("rechargePower"),
        );

        this.activePower = values.activePower;
        this.peakShavingPower = values.peakShavingPower;
        this.rechargePower = values.rechargePower;
    }
}
