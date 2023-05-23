import { Component } from '@angular/core';
import { AbstractFlatWidget } from 'src/app/shared/genericComponents/flat/abstract-flat-widget';
import { Utils } from 'src/app/shared/shared';
import { ModalComponent } from '../modal/modal';

@Component({
    selector: 'Controller_Ess_TimeOfUseTariff_Discharge',
    templateUrl: './flat.html'
})
export class FlatComponent extends AbstractFlatWidget {

    protected readonly CONVERT_MODE_TO_MANUAL_OFF_AUTOMATIC = Utils.CONVERT_MODE_TO_MANUAL_OFF_AUTOMATIC(this.translate);
    protected readonly CONVERT_TIME_OF_USE_TARIFF_STATE = Utils.CONVERT_TIME_OF_USE_TARIFF_STATE(this.translate);
    protected readonly CONVERT_PRICE_TO_CENT_PER_KWH = Utils.CONVERT_PRICE_TO_CENT_PER_KWH(2);

    async presentModal() {
        const modal = await this.modalController.create({
            component: ModalComponent,
            componentProps: {
                component: this.component,
            }
        });
        return await modal.present();
    }
}