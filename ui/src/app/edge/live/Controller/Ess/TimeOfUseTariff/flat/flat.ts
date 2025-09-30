// @ts-strict-ignore
import { Component, OnInit } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { ChannelAddress, Currency, CurrentData, EdgeConfig, Utils } from "src/app/shared/shared";

import { ModalComponent } from "../modal/modal";

@Component({
    selector: "Controller_Ess_TimeOfUseTariff",
    templateUrl: "./FLAT.HTML",
    standalone: false,
})
export class FlatComponent extends AbstractFlatWidget implements OnInit {

    protected readonly CONVERT_MODE_TO_MANUAL_OFF_AUTOMATIC = Utils.CONVERT_MODE_TO_MANUAL_OFF_AUTOMATIC(THIS.TRANSLATE);
    protected readonly CONVERT_TIME_OF_USE_TARIFF_STATE = Utils.CONVERT_TIME_OF_USE_TARIFF_STATE(THIS.TRANSLATE);

    protected priceWithCurrency: string = "-";

    async presentModal() {
        const modal = await THIS.MODAL_CONTROLLER.CREATE({
            component: ModalComponent,
            componentProps: {
                component: THIS.COMPONENT,
            },
        });
        return await MODAL.PRESENT();
    }

    protected override getChannelAddresses(): ChannelAddress[] {
        return [
            new ChannelAddress(THIS.COMPONENT.ID, "QuarterlyPrices"),
        ];
    }

    protected override onCurrentData(currentData: CurrentData): void {
        const quarterlyPrice = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/QuarterlyPrices"];
        const meta: EDGE_CONFIG.COMPONENT = THIS.CONFIG?.getComponent("_meta");
        const currency: string = THIS.CONFIG?.getPropertyFromComponent<string>(meta, "currency");
        const currencyLabel: CURRENCY.LABEL = CURRENCY.GET_CURRENCY_LABEL_BY_CURRENCY(currency);
        THIS.PRICE_WITH_CURRENCY = Utils.CONVERT_PRICE_TO_CENT_PER_KWH(2, currencyLabel)(quarterlyPrice);
    }
}
