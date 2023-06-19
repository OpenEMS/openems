import { Component } from '@angular/core';
import { ChannelAddress, CurrentData, Utils } from '../../../../../shared/shared';
import { Controller_Symmetric_PeakShavingModalComponent } from './modal/modal.component';
import { AbstractFlatWidget } from 'src/app/shared/genericComponents/flat/abstract-flat-widget';

@Component({
    selector: 'Controller_Symmetric_PeakShaving',
    templateUrl: './Symmetric.html'
})
export class Controller_Symmetric_PeakShavingComponent extends AbstractFlatWidget {

    public activePower: number;
    public peakShavingPower: number;
    public rechargePower: number;
    public readonly CONVERT_WATT_TO_KILOWATT = Utils.CONVERT_WATT_TO_KILOWATT;

    protected getChannelAddresses() {
        return [
            new ChannelAddress(this.component.properties['meter.id'], 'ActivePower'),
            new ChannelAddress(this.componentId, '_PropertyPeakShavingPower'),
            new ChannelAddress(this.componentId, '_PropertyRechargePower')
        ];
    }

    protected onCurrentData(currentData: CurrentData) {

        // Show 0 for negative activePower
        this.activePower = currentData.allComponents[this.component.properties['meter.id'] + '/ActivePower'] >= 0
            ? currentData.allComponents[this.component.properties['meter.id'] + '/ActivePower'] : 0;
        this.peakShavingPower = this.component.properties['peakShavingPower'];
        this.rechargePower = this.component.properties['rechargePower'];
    }

    async presentModal() {
        const modal = await this.modalController.create({
            component: Controller_Symmetric_PeakShavingModalComponent,
            componentProps: {
                component: this.component,
                edge: this.edge
            }
        });
        return await modal.present();
    }
}