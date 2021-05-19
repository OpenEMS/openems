import { Component } from '@angular/core';
import { SelfconsumptionModalComponent } from './modal/modal.component';
import { AbstractFlatWidget } from '../flat/abstract-flat-widget';
import { ChannelAddress, CurrentData, Utils } from 'src/app/shared/shared';

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
        this.calculatedSelfConsumption = this.calculateSelfConsumption(
            currentData.allComponents[SelfConsumptionComponent.SUM_GRID_ACTIVE_POWER.toString()],
            currentData.allComponents[SelfConsumptionComponent.SUM_PRODUCTION_ACTIVE_POWER.toString()])
    }

    public calculateSelfConsumption(sellToGrid: number, productionActivePower: number): number | null {
        if (sellToGrid != null && productionActivePower != null) {
            if (productionActivePower <= 0) {
                /* avoid divide by zero; production == 0 -> selfconsumption 0 % */
                return null;
            } else {
                if (sellToGrid < 0) {
                    return /* min 0 */ Math.max(0,
                        /* calculate selfconsumption */
                        (1 - sellToGrid * -1 / productionActivePower) * 100)
                } else {
                    return 100
                }
            }
        } else {
            return null;
        }
    }

    async presentModal() {
        const modal = await this.modalController.create({
            component: SelfconsumptionModalComponent,
        });
        return await modal.present();
    }
}
