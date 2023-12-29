import { Component } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { AbstractModal } from 'src/app/shared/genericComponents/modal/abstractModal';
import { ChannelAddress, Currency, CurrentData } from 'src/app/shared/shared';

export enum Mode {
    CHARGE_CONSUMPTION = 'CHARGE_CONSUMPTION',
    DELAY_DISCHARGE = 'DELAY_DISCHARGE'
}

@Component({
    templateUrl: './modal.html',
})
export class ModalComponent extends AbstractModal {

    protected readonly CONVERT_TIME_OF_USE_TARIFF_STATE = this.Utils.CONVERT_TIME_OF_USE_TARIFF_STATE(this.translate);
    protected priceWithCurrency: any;

    protected override getFormGroup(): FormGroup {
        return this.formBuilder.group({
            mode: new FormControl(this.component.properties.mode),
            controlMode: new FormControl(this.component.properties.controlMode),
            chargeConsumptionIsActive: new FormControl(this.component.properties.controlMode === Mode.CHARGE_CONSUMPTION ? true : false),
        });
    }

    protected override getChannelAddresses(): ChannelAddress[] {
        return [
            new ChannelAddress(this.component.id, 'QuarterlyPrices'),
        ];
    }

    protected override onIsInitialized(): void {
        this.subscription.add(this.formGroup?.get('chargeConsumptionIsActive').valueChanges.subscribe(isActive => {
            const mode: Mode = isActive ? Mode.CHARGE_CONSUMPTION : Mode.DELAY_DISCHARGE;
            this.formGroup.controls['controlMode'].setValue(mode);
            this.formGroup.controls['controlMode'].markAsDirty();
        }));
    }

    protected override onCurrentData(currentData: CurrentData): void {
        var quarterlyPrice = currentData.allComponents[this.component.id + '/QuarterlyPrices'];
        var currencyLabel: string = Currency.getCurrencyLabelByEdgeId(this.edge?.id);
        this.priceWithCurrency = this.Utils.CONVERT_PRICE_TO_CENT_PER_KWH(2, currencyLabel)(quarterlyPrice);
    }
}
