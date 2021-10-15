import { formatNumber } from '@angular/common';
import { Component } from '@angular/core';
import { AbstractFlatWidget } from 'src/app/shared/Generic_Components/flat/abstract-flat-widget';
import { ChannelAddress, CurrentData, Utils } from 'src/app/shared/shared';
import { TimeOfUseTariffDischargeModalComponent } from './modal/modal.component';

@Component({
    selector: 'Controller_Ess_TimeOfUseTariff_Discharge',
    templateUrl: './Ess_Time-Of-Use-Tariff_Discharge.html'
})
export class Controller_Ess_TimeOfUseTariff_Discharge extends AbstractFlatWidget {

    public state: string;
    public mode: string;
    public priceConverter = (value: any): string => {
        if (!value) {
            return '- €/kWh';
        }
        return formatNumber(value / 1000, 'de', '1.0-2') + ' €/kWh'
    }

    protected onCurrentData(currentData: CurrentData) {

        // State
        let channelState = currentData.thisComponent['StateMachine'];
        switch (channelState) {
            case -1:
                this.state = this.translate.instant('Edge.Index.Widgets.TimeOfUseTariff.State.notStarted')
                break;
            case 0:
                this.state = this.translate.instant('Edge.Index.Widgets.TimeOfUseTariff.State.delayed')
                break;
            case 1:
                this.state = this.translate.instant('Edge.Index.Widgets.TimeOfUseTariff.State.allowsDischarge')
                break;
            case 2:
                this.state = this.translate.instant('Edge.Index.Widgets.TimeOfUseTariff.State.standby')
                break;
        }

        // Mode
        let modeValue = currentData.allComponents[this.component.id + '/_PropertyMode']
        switch (modeValue) {
            case 'OFF':
                this.mode = this.translate.instant('General.off');
                break;
            case 'AUTOMATIC':
                this.mode = this.translate.instant('General.automatic');
        }
    }

    protected getChannelAddresses() {
        return [
            new ChannelAddress(this.componentId, 'Delayed'),
            new ChannelAddress(this.componentId, 'QuarterlyPrices'),
            new ChannelAddress(this.componentId, 'StateMachine'),
            new ChannelAddress(this.componentId, '_PropertyMode'),
        ]
    }

    // Once the functionality to calculate the prices and predictions at any time, this block can be uncommented.
    // async presentModal() {
    //     const modal = await this.modalController.create({
    //         component: TimeOfUseTariffDischargeModalComponent,
    //         componentProps: {
    //             component: this.component,
    //             edge: this.edge,
    //             config: this.config,
    //         }
    //     });
    //     return await modal.present();
    // }
}