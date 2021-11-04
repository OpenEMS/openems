import { formatNumber } from '@angular/common';
import { Component } from '@angular/core';
import { AbstractFlatWidget } from 'src/app/shared/genericComponents/flat/abstract-flat-widget';
import { ChannelAddress, CurrentData } from 'src/app/shared/shared';
import { Controller_Ess_TimeOfUseTariff_DischargeModalComponent } from './modal/modal.component';

@Component({
    selector: 'Controller_Ess_TimeOfUseTariff_Discharge',
    templateUrl: './Ess_Time-Of-Use-Tariff_Discharge.html'
})
export class Controller_Ess_TimeOfUseTariff_Discharge extends AbstractFlatWidget {

    public state: string;
    public mode: string;
    public priceConverter = (value: any): string => {
        if (!value) {
            return '- Cent/kWh';
        }
        return formatNumber(value / 10, 'de', '1.0-2') + ' Cent/kWh'
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

    async presentModal() {
        const modal = await this.modalController.create({
            component: Controller_Ess_TimeOfUseTariff_DischargeModalComponent,
            componentProps: {
                component: this.component,
                edge: this.edge,
                config: this.config,
            }
        });
        return await modal.present();
    }
}