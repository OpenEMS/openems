import { Component } from '@angular/core';
import { AbstractModal } from 'src/app/shared/genericComponents/modal/abstractModal';

type ChargeMode = 'FORCE_CHARGE' | 'EXCESS_POWER' | 'OFF';
@Component({
    templateUrl: './popover.html',
})

export class PopoverComponent extends AbstractModal {
    public chargeMode: ChargeMode;
}
