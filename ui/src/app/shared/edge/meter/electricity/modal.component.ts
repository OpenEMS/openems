import { Component, OnInit } from '@angular/core';
import { AbstractModalLine } from 'src/app/shared/genericComponents/modal/abstract-modal-line';
import { TextIndentation } from 'src/app/shared/genericComponents/modal/modal-line/modal-line';
import { ChannelAddress, CurrentData, Utils } from 'src/app/shared/shared';
import { Role } from 'src/app/shared/type/role';

@Component({
    selector: 'oe-electricity-meter',
    templateUrl: './modal.component.html'
})
export class ElectricityMeterComponent extends AbstractModalLine implements OnInit {

    protected override readonly Role = Role;
    protected override readonly Utils = Utils;
    protected readonly TextIndentation = TextIndentation;

    protected readonly phases: { key: string, name: string, power: number | null, current: number | null, voltage: number | null }[] = [
        { key: "L1", name: "", power: null, current: null, voltage: null },
        { key: "L2", name: "", power: null, current: null, voltage: null },
        { key: "L3", name: "", power: null, current: null, voltage: null }
    ];

    protected override getChannelAddresses(): ChannelAddress[] {
        let channelAddresses: ChannelAddress[] = [];
        for (let phase of [1, 2, 3]) {
            channelAddresses.push(
                new ChannelAddress(this.component.id, 'CurrentL' + phase),
                new ChannelAddress(this.component.id, 'VoltageL' + phase),
                new ChannelAddress(this.component.id, 'ActivePowerL' + phase)
            );
        }
        return channelAddresses;
    }

    protected override onCurrentData(currentData: CurrentData): void {
        this.phases.forEach((phase) => {
            var power = currentData.allComponents[this.component.id + '/ActivePower' + phase.key];
            phase.name = "Phase " + phase.key;
            phase.power = Utils.absSafely(power);
            phase.current = currentData.allComponents[this.component.id + '/Current' + phase.key];
            phase.voltage = currentData.allComponents[this.component.id + '/Voltage' + phase.key];
        });
    }
}
