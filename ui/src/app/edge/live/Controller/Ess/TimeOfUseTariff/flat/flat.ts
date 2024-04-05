import { Component, OnInit } from '@angular/core';
import { AbstractFlatWidget } from 'src/app/shared/genericComponents/flat/abstract-flat-widget';
import { ChannelAddress, Currency, CurrentData, Utils } from 'src/app/shared/shared';

import { ModalComponent } from '../modal/modal';

@Component({
    selector: 'Controller_Ess_TimeOfUseTariff',
    templateUrl: './flat.html',
})
export class FlatComponent extends AbstractFlatWidget implements OnInit {

    protected readonly CONVERT_MODE_TO_MANUAL_OFF_AUTOMATIC = Utils.CONVERT_MODE_TO_MANUAL_OFF_AUTOMATIC(this.translate);
    protected readonly CONVERT_TIME_OF_USE_TARIFF_STATE = Utils.CONVERT_TIME_OF_USE_TARIFF_STATE(this.translate);
    protected priceWithCurrency: any;

    async presentModal() {
        const modal = await this.modalController.create({
            component: ModalComponent,
            componentProps: {
                component: this.component,
            },
        });
        return await modal.present();
    }

    protected override getChannelAddresses(): ChannelAddress[] {
        return [
            new ChannelAddress(this.component.id, 'QuarterlyPrices'),
        ];
    }

    protected override onCurrentData(currentData: CurrentData): void {
        const quarterlyPrice = currentData.allComponents[this.component.id + '/QuarterlyPrices'];
        const currencyLabel: string = Currency.getCurrencyLabelByEdgeId(this.edge.id);
        this.priceWithCurrency = Utils.CONVERT_PRICE_TO_CENT_PER_KWH(2, currencyLabel)(quarterlyPrice);
    }
}
