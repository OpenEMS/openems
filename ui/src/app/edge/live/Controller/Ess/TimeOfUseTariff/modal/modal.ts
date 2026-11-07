import { ChangeDetectionStrategy, Component } from "@angular/core";
import { FormControl, FormGroup } from "@angular/forms";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { ChannelAddress, Currency, CurrentData, EdgeConfig } from "src/app/shared/shared";
import { Controller_Ess_TimeOfUseTariffUtils } from "../utils";

@Component({
    templateUrl: "./modal.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class ModalComponent extends AbstractModal {
    protected readonly CONVERT_TIME_OF_USE_TARIFF_STATE = this.Utils.CONVERT_TIME_OF_USE_TARIFF_STATE(this.translate);
    protected priceWithCurrency: string = "-";

    protected override getFormGroup(): FormGroup {
        return this.formBuilder.group({
            mode: new FormControl(this.getModalComponent().properties.mode),
            controlMode: new FormControl(this.getModalComponent().properties.controlMode),
            chargeConsumptionIsActive: new FormControl(
                this.getModalComponent().properties.controlMode ===
                    Controller_Ess_TimeOfUseTariffUtils.ControlMode.CHARGE_CONSUMPTION,
            ),
        });
    }

    protected override getChannelAddresses(): ChannelAddress[] {
        return [new ChannelAddress(this.getModalComponent().id, "QuarterlyPrices")];
    }

    protected override onIsInitialized(): void {
        this.subscription.add(
            this.formGroup!.get("chargeConsumptionIsActive")!.valueChanges.subscribe((isActive) => {
                const controlMode: Controller_Ess_TimeOfUseTariffUtils.ControlMode = isActive
                    ? Controller_Ess_TimeOfUseTariffUtils.ControlMode.CHARGE_CONSUMPTION
                    : Controller_Ess_TimeOfUseTariffUtils.ControlMode.DELAY_DISCHARGE;
                this.formGroup!.controls["controlMode"]!.setValue(controlMode);
                this.formGroup!.controls["controlMode"]!.markAsDirty();
            }),
        );
    }

    protected override onCurrentData(currentData: CurrentData): void {
        const quarterlyPrice = currentData.allComponents[this.getModalComponent().id + "/QuarterlyPrices"];
        const meta: EdgeConfig.Component = this.config?.getComponent("_meta");
        if (meta == null) {
            return;
        }
        const currency: string | null = this.config?.getPropertyFromComponent<string>(meta, "currency");
        if (currency == null) {
            return;
        }
        const currencyLabel: Currency.Label = Currency.getCurrencyLabelByCurrency(currency);
        this.priceWithCurrency = this.Utils.CONVERT_PRICE_TO_CENT_PER_KWH(2, currencyLabel)(quarterlyPrice);
    }
}
