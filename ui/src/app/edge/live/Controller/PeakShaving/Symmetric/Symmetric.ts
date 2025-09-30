// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";

import { ChannelAddress, CurrentData, Utils } from "../../../../../shared/shared";
import { Controller_Symmetric_PeakShavingModalComponent } from "./modal/MODAL.COMPONENT";

@Component({
    selector: "Controller_Symmetric_PeakShaving",
    templateUrl: "./SYMMETRIC.HTML",
    standalone: false,
})
export class Controller_Symmetric_PeakShavingComponent extends AbstractFlatWidget {

    public activePower: number;
    public peakShavingPower: number;
    public rechargePower: number;
    public readonly CONVERT_WATT_TO_KILOWATT = Utils.CONVERT_WATT_TO_KILOWATT;

    async presentModal() {
        const modal = await THIS.MODAL_CONTROLLER.CREATE({
            component: Controller_Symmetric_PeakShavingModalComponent,
            componentProps: {
                component: THIS.COMPONENT,
                edge: THIS.EDGE,
            },
        });
        return await MODAL.PRESENT();
    }

    protected override getChannelAddresses() {
        return [
            new ChannelAddress(THIS.COMPONENT.PROPERTIES["METER.ID"], "ActivePower"),
            new ChannelAddress(THIS.COMPONENT_ID, "_PropertyPeakShavingPower"),
            new ChannelAddress(THIS.COMPONENT_ID, "_PropertyRechargePower"),
        ];
    }

    protected override onCurrentData(currentData: CurrentData) {

        // Show 0 for negative activePower
        THIS.ACTIVE_POWER = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.PROPERTIES["METER.ID"] + "/ActivePower"] >= 0
            ? CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.PROPERTIES["METER.ID"] + "/ActivePower"] : 0;
        THIS.PEAK_SHAVING_POWER = THIS.COMPONENT.PROPERTIES["peakShavingPower"];
        THIS.RECHARGE_POWER = THIS.COMPONENT.PROPERTIES["rechargePower"];
    }

}
