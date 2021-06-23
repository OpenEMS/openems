import { Component } from '@angular/core';
import { ChannelAddress, CurrentData, Utils } from 'src/app/shared/shared';
import { AbstractFlatWidget } from '../../flat/abstract-flat-widget';
import { SelfconsumptionModalComponent } from './modal/modal.component';

@Component({
    selector: 'selfconsumption',
    templateUrl: './selfconsumption.component.html'
})
export class SelfConsumptionComponent extends AbstractFlatWidget {

    private static readonly SUM_GRID_ACTIVE_POWER: ChannelAddress = new ChannelAddress('_sum', 'GridActivePower')
    private static readonly SUM_PRODUCTION_ACTIVE_POWER: ChannelAddress = new ChannelAddress('_sum', 'ProductionActivePower')
    public calculatedSelfConsumption: number;

    protected getChannelAddresses() {
        return [SelfConsumptionComponent.SUM_GRID_ACTIVE_POWER, SelfConsumptionComponent.SUM_PRODUCTION_ACTIVE_POWER]
    }

    protected onCurrentData(currentData: CurrentData) {
        this.calculatedSelfConsumption = Utils.calculateSelfConsumption(
            Utils.multiplySafely(currentData.allComponents[SelfConsumptionComponent.SUM_GRID_ACTIVE_POWER.toString()], -1),
            currentData.allComponents[SelfConsumptionComponent.SUM_PRODUCTION_ACTIVE_POWER.toString()])
    }

    async presentModal() {
        const modal = await this.modalController.create({
            component: SelfconsumptionModalComponent,
        });
        return await modal.present();
    }
}
