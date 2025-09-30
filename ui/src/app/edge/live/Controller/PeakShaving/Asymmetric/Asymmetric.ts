// @ts-strict-ignore
import { Component } from "@angular/core";
import { BehaviorSubject } from "rxjs";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";

import { ChannelAddress, CurrentData, Utils } from "../../../../../shared/shared";
import { Controller_Asymmetric_PeakShavingModalComponent } from "./modal/MODAL.COMPONENT";

@Component({
    selector: "Controller_Asymmetric_PeakShaving",
    templateUrl: "./ASYMMETRIC.HTML",
    standalone: false,
})
export class Controller_Asymmetric_PeakShavingComponent extends AbstractFlatWidget {

    public mostStressedPhase: BehaviorSubject<{ name: string, value: number }> = new BehaviorSubject(null);
    public meterId: string;
    public peakShavingPower: number;
    public rechargePower: number;
    public readonly CONVERT_WATT_TO_KILOWATT = Utils.CONVERT_WATT_TO_KILOWATT;

    async presentModal() {
        const modal = await THIS.MODAL_CONTROLLER.CREATE({
            component: Controller_Asymmetric_PeakShavingModalComponent,
            componentProps: {
                component: THIS.COMPONENT,
                edge: THIS.EDGE,
                mostStressedPhase: THIS.MOST_STRESSED_PHASE,
            },
        });
        return await MODAL.PRESENT();
    }

    protected override getChannelAddresses() {
        THIS.METER_ID = THIS.COMPONENT.PROPERTIES["METER.ID"];
        return [
            new ChannelAddress(THIS.METER_ID, "ActivePower"),
            new ChannelAddress(THIS.METER_ID, "ActivePowerL1"),
            new ChannelAddress(THIS.METER_ID, "ActivePowerL2"),
            new ChannelAddress(THIS.METER_ID, "ActivePowerL3"),
        ];
    }

    protected override onCurrentData(currentData: CurrentData) {

        const activePowerArray: number[] = [
            CURRENT_DATA.ALL_COMPONENTS[THIS.METER_ID + "/ActivePowerL1"],
            CURRENT_DATA.ALL_COMPONENTS[THIS.METER_ID + "/ActivePowerL2"],
            CURRENT_DATA.ALL_COMPONENTS[THIS.METER_ID + "/ActivePowerL3"],
        ];

        const name: string[] = ["L1", "L2", "L3"];

        THIS.MOST_STRESSED_PHASE.NEXT({

            // Show most stressed Phase
            name: name[ACTIVE_POWER_ARRAY.INDEX_OF(MATH.MAX(...activePowerArray))],
            value: MATH.MAX(...activePowerArray, 0),
        });

        THIS.PEAK_SHAVING_POWER = THIS.COMPONENT.PROPERTIES["peakShavingPower"];
        THIS.RECHARGE_POWER = THIS.COMPONENT.PROPERTIES["rechargePower"];
    }

}
