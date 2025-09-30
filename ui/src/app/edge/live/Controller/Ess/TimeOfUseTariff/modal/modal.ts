// @ts-strict-ignore
import { Component } from "@angular/core";
import { FormControl, FormGroup } from "@angular/forms";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { ChannelAddress, Currency, CurrentData, EdgeConfig } from "src/app/shared/shared";
import { Controller_Ess_TimeOfUseTariffUtils } from "../utils";

@Component({
    templateUrl: "./MODAL.HTML",
    standalone: false,
})
export class ModalComponent extends AbstractModal {

    protected readonly CONVERT_TIME_OF_USE_TARIFF_STATE = THIS.UTILS.CONVERT_TIME_OF_USE_TARIFF_STATE(THIS.TRANSLATE);
    protected priceWithCurrency: string;

    protected override getFormGroup(): FormGroup {
        return THIS.FORM_BUILDER.GROUP({
            mode: new FormControl(THIS.COMPONENT.PROPERTIES.MODE),
            controlMode: new FormControl(THIS.COMPONENT.PROPERTIES.CONTROL_MODE),
            chargeConsumptionIsActive: new FormControl(THIS.COMPONENT.PROPERTIES.CONTROL_MODE === Controller_Ess_TimeOfUseTariffUtils.ControlMode.CHARGE_CONSUMPTION ? true : false),
        });
    }

    protected override getChannelAddresses(): ChannelAddress[] {
        return [
            new ChannelAddress(THIS.COMPONENT.ID, "QuarterlyPrices"),
        ];
    }

    protected override onIsInitialized(): void {
        THIS.SUBSCRIPTION.ADD(
            THIS.FORM_GROUP?.get("chargeConsumptionIsActive")
                .valueChanges
                .subscribe(isActive => {
                    const controlMode: Controller_Ess_TimeOfUseTariffUtils.ControlMode = isActive
                        ? Controller_Ess_TimeOfUseTariffUtils.ControlMode.CHARGE_CONSUMPTION
                        : Controller_Ess_TimeOfUseTariffUtils.ControlMode.DELAY_DISCHARGE;
                    THIS.FORM_GROUP.CONTROLS["controlMode"].setValue(controlMode);
                    THIS.FORM_GROUP.CONTROLS["controlMode"].markAsDirty();
                }));
    }

    protected override onCurrentData(currentData: CurrentData): void {
        const quarterlyPrice = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/QuarterlyPrices"];
        const meta: EDGE_CONFIG.COMPONENT = THIS.CONFIG?.getComponent("_meta");
        const currency: string = THIS.CONFIG?.getPropertyFromComponent<string>(meta, "currency");
        const currencyLabel: CURRENCY.LABEL = CURRENCY.GET_CURRENCY_LABEL_BY_CURRENCY(currency);
        THIS.PRICE_WITH_CURRENCY = THIS.UTILS.CONVERT_PRICE_TO_CENT_PER_KWH(2, currencyLabel)(quarterlyPrice);
    }
}
