// @ts-strict-ignore
import { Component } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { AbstractFlatWidget } from 'src/app/shared/components/flat/abstract-flat-widget';

import { ChannelAddress, CurrentData, Utils } from '../../../../../shared/shared';
import { Controller_Asymmetric_PeakShavingModalComponent } from './modal/modal.component';

@Component({
    selector: 'Controller_Asymmetric_PeakShaving',
    templateUrl: './Asymmetric.html',
})
export class Controller_Asymmetric_PeakShavingComponent extends AbstractFlatWidget {

    public mostStressedPhase: BehaviorSubject<{ name: string, value: number }> = new BehaviorSubject(null);
    public meterId: string;
    public peakShavingPower: number;
    public rechargePower: number;
    public readonly CONVERT_WATT_TO_KILOWATT = Utils.CONVERT_WATT_TO_KILOWATT;

    protected override getChannelAddresses() {
        this.meterId = this.component.properties['meter.id'];
        return [
            new ChannelAddress(this.meterId, 'ActivePower'),
            new ChannelAddress(this.meterId, 'ActivePowerL1'),
            new ChannelAddress(this.meterId, 'ActivePowerL2'),
            new ChannelAddress(this.meterId, 'ActivePowerL3'),
        ];
    }

    protected override onCurrentData(currentData: CurrentData) {

        const activePowerArray: number[] = [
            currentData.allComponents[this.meterId + '/ActivePowerL1'],
            currentData.allComponents[this.meterId + '/ActivePowerL2'],
            currentData.allComponents[this.meterId + '/ActivePowerL3'],
        ];

        const name: string[] = ['L1', 'L2', 'L3'];

        this.mostStressedPhase.next({

            // Show most stressed Phase
            name: name[activePowerArray.indexOf(Math.max(...activePowerArray))],
            value: Math.max(...activePowerArray, 0),
        });

        this.peakShavingPower = this.component.properties['peakShavingPower'];
        this.rechargePower = this.component.properties['rechargePower'];
    }

    async presentModal() {
        const modal = await this.modalController.create({
            component: Controller_Asymmetric_PeakShavingModalComponent,
            componentProps: {
                component: this.component,
                edge: this.edge,
                mostStressedPhase: this.mostStressedPhase,
            },
        });
        return await modal.present();
    }
}
