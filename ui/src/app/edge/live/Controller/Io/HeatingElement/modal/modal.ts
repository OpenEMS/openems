import { Component, OnInit } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { BehaviorSubject } from 'rxjs';
import { AbstractModal } from 'src/app/shared/genericComponents/modal/abstractModal';
import { ChannelAddress, CurrentData } from 'src/app/shared/shared';
import { Mode, WorkMode } from 'src/app/shared/type/general';

@Component({
    selector: 'heatingelement-modal',
    templateUrl: './modal.html',
})
export class ModalComponent extends AbstractModal implements OnInit {

    private static PROPERTY_MODE: string = '_PropertyMode';
    protected activePhases: BehaviorSubject<number> = new BehaviorSubject(0);
    protected mode: string;
    protected state: string;
    protected outputChannelArray: ChannelAddress[] = [];

    protected readonly Mode = Mode;
    protected readonly WorkMode = WorkMode;

    protected override getChannelAddresses(): ChannelAddress[] {
        let outputChannelPhaseOne = ChannelAddress.fromString(
            this.component.properties['outputChannelPhaseL1']);
        let outputChannelPhaseTwo = ChannelAddress.fromString(
            this.component.properties['outputChannelPhaseL2']);
        let outputChannelPhaseThree = ChannelAddress.fromString(
            this.component.properties['outputChannelPhaseL3']);
        this.outputChannelArray = [outputChannelPhaseOne, outputChannelPhaseTwo, outputChannelPhaseThree];

        let channelAddresses: ChannelAddress[] = [
            new ChannelAddress(this.component.id, 'ForceStartAtSecondsOfDay'),
            outputChannelPhaseOne,
            outputChannelPhaseTwo,
            outputChannelPhaseThree,
            new ChannelAddress(this.component.id, ModalComponent.PROPERTY_MODE),
            new ChannelAddress(this.component.id, '_PropertyWorkMode')
        ]
        return channelAddresses
    }

    protected override onCurrentData(currentData: CurrentData) {

        // get current mode
        this.mode = currentData.allComponents[this.component.id + '/' + ModalComponent.PROPERTY_MODE];

        let value = 0;
        this.outputChannelArray.forEach(element => {
            if (currentData.allComponents[element.toString()] == 1) {
                value += 1;
            }
        })

        // Get current state
        this.activePhases.next(value);
        if (this.activePhases.value > 0) {
            this.state = this.translate.instant('General.active');
        } else if (this.activePhases.value == 0) {
            this.state = this.translate.instant('General.inactive');
        }
    }

    protected override getFormGroup(): FormGroup {
        return this.formBuilder.group({
            minTime: new FormControl(this.component.properties.minTime),
            minKwh: new FormControl(this.component.properties.minKwh),
            endTime: new FormControl(this.component.properties.endTime),
            workMode: new FormControl(this.component.properties.workMode),
            defaultLevel: new FormControl(this.component.properties.defaultLevel),
            mode: new FormControl(this.mode)
        })
    }

    // allowMinimumHeating == workMode: none
    // TODO remove when outputting of event is errorless possible
    switchAllowMinimumHeating(event: CustomEvent) {
        if (event.detail.checked == true) {
            this.formGroup.controls['workMode'].setValue('TIME');
            this.formGroup.controls['workMode'].markAsDirty()
        } else if (event.detail.checked == false) {
            this.formGroup.controls['workMode'].setValue('NONE');
            this.formGroup.controls['workMode'].markAsDirty()
        }
    }
}