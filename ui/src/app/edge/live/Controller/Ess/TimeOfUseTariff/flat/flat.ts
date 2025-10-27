// @ts-strict-ignore
import { Component, OnInit } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { Modal } from "src/app/shared/components/flat/flat";
import { ChannelAddress, Currency, CurrentData, EdgeConfig, Utils } from "src/app/shared/shared";

import { ModalComponent } from "../modal/modal";

@Component({
    selector: "Controller_Ess_TimeOfUseTariff",
    templateUrl: "./flat.html",
    standalone: false,
})
export class FlatComponent extends AbstractFlatWidget implements OnInit {

    protected readonly CONVERT_MODE_TO_MANUAL_OFF_AUTOMATIC = Utils.CONVERT_MODE_TO_MANUAL_OFF_AUTOMATIC(this.translate);
    protected readonly CONVERT_TIME_OF_USE_TARIFF_STATE = Utils.CONVERT_TIME_OF_USE_TARIFF_STATE(this.translate);

    protected priceWithCurrency: string = "-";
    protected modalComponent: Modal | null = null;

    protected override afterIsInitialized(): void {
        this.modalComponent = this.getModalComponent();
    }

    protected getModalComponent(): Modal {
        return {
            component: ModalComponent,
            componentProps: {
                component: this.component,
            },
        };
    };

    protected override getChannelAddresses(): ChannelAddress[] {
        return [
            new ChannelAddress(this.component.id, "QuarterlyPrices"),
        ];
    }

    protected override onCurrentData(currentData: CurrentData): void {
        const quarterlyPrice = currentData.allComponents[this.component.id + "/QuarterlyPrices"];
        const meta: EdgeConfig.Component = this.config?.getComponent("_meta");
        const currency: string = this.config?.getPropertyFromComponent<string>(meta, "currency");
        const currencyLabel: Currency.Label = Currency.getCurrencyLabelByCurrency(currency);
        this.priceWithCurrency = Utils.CONVERT_PRICE_TO_CENT_PER_KWH(2, currencyLabel)(quarterlyPrice);
    }
}
