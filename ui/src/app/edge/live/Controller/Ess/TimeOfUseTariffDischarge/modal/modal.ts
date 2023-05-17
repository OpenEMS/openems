import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { AbstractModal } from 'src/app/shared/genericComponents/modal/abstractModal';
import { ChannelAddress, Currency, CurrentData, EdgeConfig } from 'src/app/shared/shared';

@Component({
    templateUrl: './modal.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ModalComponent extends AbstractModal {

    @Input() public component: EdgeConfig.Component;

    protected readonly CONVERT_TIME_OF_USE_TARIFF_STATE = this.Utils.CONVERT_TIME_OF_USE_TARIFF_STATE(this.translate);
    protected storageStatuslabel: string;
    protected priceWithCurrency: any;

    protected override getFormGroup(): FormGroup {
        return this.formBuilder.group({
            mode: new FormControl(this.component.properties.mode)
        });
    }

    protected override getChannelAddresses(): ChannelAddress[] {
        return [
            new ChannelAddress(this.component.id, 'QuarterlyPrices')
        ];
    }

    protected override onCurrentData(currentData: CurrentData): void {
        var quarterlyPrice = currentData.allComponents[this.component.id + '/QuarterlyPrices'];

        var currencyLabel: string = Currency.getCurrencyLabelByEdgeId(this.edge?.id);

        // Since 'component' is empty during ngOninit. so assigning the labels through this method.
        this.storageStatuslabel = this.Utils.getTimeOfUseTariffStorageLabel(this.component, this.translate);
        this.priceWithCurrency = this.Utils.CONVERT_PRICE_TO_CENT_PER_KWH(2, currencyLabel)(quarterlyPrice);
    }
}
