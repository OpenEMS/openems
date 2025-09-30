import { Component, OnInit } from "@angular/core";
import { AbstractModalLine } from "src/app/shared/components/modal/abstract-modal-line";
import { TextIndentation } from "src/app/shared/components/modal/modal-line/modal-line";
import { ChannelAddress, CurrentData, Utils } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";

@Component({
    selector: "oe-electricity-meter",
    templateUrl: "./MODAL.COMPONENT.HTML",
    standalone: false,
})
export class ElectricityMeterComponent extends AbstractModalLine implements OnInit {

    protected override readonly Role = Role;
    protected override readonly Utils = Utils;
    protected readonly TextIndentation = TextIndentation;

    protected readonly phases: { key: string, name: string, power: number | null, current: number | null, voltage: number | null }[] = [
        { key: "L1", name: "", power: null, current: null, voltage: null },
        { key: "L2", name: "", power: null, current: null, voltage: null },
        { key: "L3", name: "", power: null, current: null, voltage: null },
    ];

    protected override getChannelAddresses(): ChannelAddress[] {
        const channelAddresses: ChannelAddress[] = [];
        for (const phase of [1, 2, 3]) {
            CHANNEL_ADDRESSES.PUSH(
                new ChannelAddress(THIS.COMPONENT.ID, "CurrentL" + phase),
                new ChannelAddress(THIS.COMPONENT.ID, "VoltageL" + phase),
                new ChannelAddress(THIS.COMPONENT.ID, "ActivePowerL" + phase),
            );
        }
        return channelAddresses;
    }

    protected override onCurrentData(currentData: CurrentData): void {
        THIS.PHASES.FOR_EACH((phase) => {
            const power = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/ActivePower" + PHASE.KEY];
            PHASE.NAME = "Phase " + PHASE.KEY;
            PHASE.POWER = UTILS.ABS_SAFELY(power);
            PHASE.CURRENT = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/Current" + PHASE.KEY];
            PHASE.VOLTAGE = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/Voltage" + PHASE.KEY];
        });
    }
}
