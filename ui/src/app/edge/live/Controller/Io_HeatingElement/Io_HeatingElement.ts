import { ChannelAddress, EdgeConfig, CurrentData, Utils } from '../../../../shared/shared';
import { Component } from '@angular/core';
import { Controller_Io_HeatingElementModalComponent } from './modal/modal.component';
import { BehaviorSubject } from 'rxjs';
import { AbstractFlatWidget } from 'src/app/shared/genericComponents/flat/abstract-flat-widget';

@Component({
    selector: 'Controller_Io_HeatingElement',
    templateUrl: './Io_HeatingElement.html'
})
export class Controller_Io_HeatingElementComponent extends AbstractFlatWidget {

    public component: EdgeConfig.Component = null;
    public outputChannelPhaseOne: ChannelAddress = null;
    public outputChannelPhaseTwo: ChannelAddress = null;
    public outputChannelPhaseThree: ChannelAddress = null;
    public activePhases: BehaviorSubject<number> = new BehaviorSubject(0);
    public mode: string;
    public state: string;
    private static PROPERTY_MODE: string = '_PropertyMode'

    public readonly CONVERT_SECONDS_TO_DATE_FORMAT = Utils.CONVERT_SECONDS_TO_DATE_FORMAT;

    protected getChannelAddresses() {

        this.outputChannelPhaseOne = ChannelAddress.fromString(
            this.component.properties['outputChannelPhaseL1']);
        this.outputChannelPhaseTwo = ChannelAddress.fromString(
            this.component.properties['outputChannelPhaseL2']);
        this.outputChannelPhaseThree = ChannelAddress.fromString(
            this.component.properties['outputChannelPhaseL3']);

        let channelAddresses: ChannelAddress[] = [
            new ChannelAddress(this.component.id, 'ForceStartAtSecondsOfDay'),
            this.outputChannelPhaseOne,
            this.outputChannelPhaseTwo,
            this.outputChannelPhaseThree,
            new ChannelAddress(this.component.id, Controller_Io_HeatingElementComponent.PROPERTY_MODE)
        ]
        return channelAddresses
    }

    protected onCurrentData(currentData: CurrentData) {

        // get current mode
        let channel = currentData.thisComponent[Controller_Io_HeatingElementComponent.PROPERTY_MODE];

        switch (channel) {
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

        let outputChannelArray = [this.outputChannelPhaseOne, this.outputChannelPhaseTwo, this.outputChannelPhaseThree];

        // check if 'at least' one outputChannelPhase equals 1
        let value = 0;
        outputChannelArray.forEach(element => {
            if (currentData.allComponents[element.toString()] == 1) {
                value += 1;
            }
        })

        // Get current state
        this.activePhases.next(value);
        if (this.activePhases.value > 0) {
            this.state = 'General.active'
        } else if (this.activePhases.value == 0) {
            this.state = 'General.inactive'
        }
    }

    async presentModal() {
        const modal = await this.modalController.create({
            component: Controller_Io_HeatingElementModalComponent,
            componentProps: {
                component: this.component,
                edge: this.edge,
                activePhases: this.activePhases
            }
        });
        return await modal.present();
    }
}
