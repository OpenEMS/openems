import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { AbstractModal } from 'src/app/shared/genericComponents/modal/abstractModal';
import { ChannelAddress, EdgeConfig } from 'src/app/shared/shared';

@Component({
    templateUrl: './modal.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ModalComponent extends AbstractModal {

    @Input() public component: EdgeConfig.Component;

    protected readonly CONVERT_PRICE_TO_CENT_PER_KWH = this.Utils.CONVERT_PRICE_TO_CENT_PER_KWH(4);
    protected readonly CONVERT_TIME_OF_USE_TARIFF_STATE = this.Utils.CONVERT_TIME_OF_USE_TARIFF_STATE(this.translate);
    protected label: string;

    protected override getFormGroup(): FormGroup {
        return this.formBuilder.group({
            mode: new FormControl(this.component.properties.mode)
        });
    }

    // This method is used only to assign the 'label', since 'component' is empty during ngOninit.
    // so assigning the label through getChannelAddresses method.
    protected override getChannelAddresses(): ChannelAddress[] {
        this.label = this.Utils.getTimeOfUseTariffStorageLabel(this.component, this.translate);
        return [];
    }
}
