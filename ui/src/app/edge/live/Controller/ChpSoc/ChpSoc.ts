import { Component } from '@angular/core';
import { AbstractFlatWidget } from 'src/app/shared/genericComponents/flat/abstract-flat-widget';
import { Icon } from 'src/app/shared/type/widget';

import { ChannelAddress, CurrentData } from '../../../../shared/shared';
import { Controller_ChpSocModalComponent } from './modal/modal.component';

@Component({
    selector: 'Controller_ChpSocComponent',
    templateUrl: './ChpSoc.html',
})
export class Controller_ChpSocComponent extends AbstractFlatWidget {

    public inputChannel: ChannelAddress = null;
    public outputChannel: ChannelAddress = null;
    public propertyModeChannel: ChannelAddress = null;
    public highThresholdValue: number;
    public lowThresholdValue: number;
    public state: string;
    public mode: string;
    public modeChannelValue: string;
    public inputChannelValue: number;
    public icon: Icon = {
        name: '',
        size: 'large',
        color: 'primary',
    };
    private static PROPERTY_MODE: string = '_PropertyMode';

    protected override getChannelAddresses() {
        this.outputChannel = ChannelAddress.fromString(
            this.component.properties['outputChannelAddress']);
        this.inputChannel = ChannelAddress.fromString(
            this.component.properties['inputChannelAddress']);
        this.propertyModeChannel = new ChannelAddress(this.component.id, Controller_ChpSocComponent.PROPERTY_MODE);
        return [
            this.outputChannel,
            this.inputChannel,
            this.propertyModeChannel,
            new ChannelAddress(this.component.id, '_PropertyHighThreshold'),
            new ChannelAddress(this.component.id, '_PropertyLowThreshold'),
        ];
    }

    protected override onCurrentData(currentData: CurrentData) {

        // Mode
        this.modeChannelValue = currentData.allComponents[this.propertyModeChannel.toString()];
        switch (this.modeChannelValue) {
            case 'ON':
                this.mode = this.translate.instant('General.on');
                break;
            case 'OFF':
                this.mode = this.translate.instant('General.off');
                break;
            case 'AUTOMATIC':
                this.mode = this.translate.instant('General.automatic');
        }

        const outputChannelValue = currentData.allComponents[this.outputChannel.toString()];

        switch (outputChannelValue) {
            case 0:
                this.state = this.translate.instant('General.inactive');
                this.icon.name == 'help-outline';
                break;
            case 1:
                this.state = this.translate.instant('General.active');
                break;
        }

        this.inputChannelValue = currentData.allComponents[this.inputChannel.toString()];
        this.highThresholdValue = currentData.allComponents[this.component.id + '/_PropertyHighThreshold'];
        this.lowThresholdValue = currentData.allComponents[this.component.id + '/_PropertyLowThreshold'];
    }

    async presentModal() {
        const modal = await this.modalController.create({
            component: Controller_ChpSocModalComponent,
            componentProps: {
                component: this.component,
                edge: this.edge,
                outputChannel: this.outputChannel,
                inputChannel: this.inputChannel,
            },
        });
        return await modal.present();
    }
}
