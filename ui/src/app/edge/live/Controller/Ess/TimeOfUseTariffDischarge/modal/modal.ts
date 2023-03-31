import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { AbstractModal } from 'src/app/shared/genericComponents/modal/abstractModal';
import { Edge, EdgeConfig } from 'src/app/shared/shared';

@Component({
    templateUrl: './modal.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ModalComponent extends AbstractModal {

    @Input() public component: EdgeConfig.Component;

    protected readonly CONVERT_PRICE_TO_CENT_PER_KWH = this.Utils.CONVERT_PRICE_TO_CENT_PER_KWH(4);
    protected readonly CONVERT_TIME_OF_USE_TARIFF_STATE = this.Utils.CONVERT_TIME_OF_USE_TARIFF_STATE(this.translate);
    protected label: string = this.Utils.getTimeOfUseTariffStorageLabel(this.component, this.translate);

    protected override getFormGroup(): FormGroup {
        return this.formBuilder.group({
            mode: new FormControl(this.component.properties.mode)
        });
    }
}
