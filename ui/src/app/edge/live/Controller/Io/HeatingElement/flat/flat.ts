import { Component } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { AbstractFlatWidget } from 'src/app/shared/genericComponents/flat/abstract-flat-widget';
import { ChannelAddress, CurrentData, Utils } from 'src/app/shared/shared';
import { WorkMode } from 'src/app/shared/type/general';

import { ModalComponent } from '../modal/modal';

@Component({
    selector: 'Controller_Io_HeatingElement',
    templateUrl: './flat.html',
})
export class FlatComponent extends AbstractFlatWidget {

    private outputChannelArray: ChannelAddress[] = [];
    private static PROPERTY_MODE: string = '_PropertyMode';

    protected activePhases: BehaviorSubject<number> = new BehaviorSubject(0);
    protected mode: string;
    protected state: string;
    protected runState: Status;
    protected workMode: WorkMode;
    protected readonly WorkMode = WorkMode;
    protected readonly CONVERT_SECONDS_TO_DATE_FORMAT = Utils.CONVERT_SECONDS_TO_DATE_FORMAT;

    protected override getChannelAddresses() {

        this.outputChannelArray.push(
            ChannelAddress.fromString(
                this.component.properties['outputChannelPhaseL1']),
            ChannelAddress.fromString(
                this.component.properties['outputChannelPhaseL2']),
            ChannelAddress.fromString(
                this.component.properties['outputChannelPhaseL3']),
        );

        const channelAddresses: ChannelAddress[] = [
            new ChannelAddress(this.component.id, 'ForceStartAtSecondsOfDay'),
            ...this.outputChannelArray,
            new ChannelAddress(this.component.id, 'Status'),
            new ChannelAddress(this.component.id, FlatComponent.PROPERTY_MODE),
            new ChannelAddress(this.component.id, '_PropertyWorkMode'),
        ];
        return channelAddresses;
    }

    protected override onCurrentData(currentData: CurrentData) {

        this.workMode = currentData.allComponents[this.component.id + '/' + '_PropertyWorkMode'];

        // get current mode
        switch (currentData.allComponents[this.component.id + '/' + FlatComponent.PROPERTY_MODE]) {
            case 'MANUAL_ON': {
                this.mode = 'General.on';
                break;
            }
            case 'MANUAL_OFF': {
                this.mode = 'General.off';
                break;
            }
            case 'AUTOMATIC': {
                this.mode = 'General.automatic';
                break;
            }
        }

        // check if 'at least' one outputChannelPhase equals 1
        let value = 0;
        this.outputChannelArray.forEach(element => {
            if (currentData.allComponents[element.toString()] == 1) {
                value += 1;
            }
        });

        // Get current state
        this.activePhases.next(value);
        if (this.activePhases.value > 0) {
            this.state = 'General.active';

            // Check forced heat
            // TODO: Use only Status if edge version is latest [2022.8]
            this.runState = currentData.allComponents[this.component.id + '/' + 'Status'];

            if (this.runState == Status.ActiveForced) {
                this.state = 'Edge.Index.Widgets.Heatingelement.activeForced';
            }
        } else if (this.activePhases.value == 0) {
            this.state = 'General.inactive';
        }
    }

    async presentModal() {
        const modal = await this.modalController.create({
            component: ModalComponent,
            componentProps: {
                component: this.component,
            },
        });
        return await modal.present();
    }
}

export enum Status {
    "Undefined" = -1,
    "Inactive" = 0,
    "Active" = 1,
    "ActiveForced" = 2
}
