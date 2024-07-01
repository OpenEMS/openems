import { Component, Input } from '@angular/core';
import { Converter } from 'src/app/shared/genericComponents/shared/converter';
import { Utils } from 'src/app/shared/shared';
import { Role } from 'src/app/shared/type/role';
import { EdgeConfig } from '../../edgeconfig';

@Component({
    selector: 'oe-ess-charger',
    templateUrl: './modal.component.html',
})
export class EssChargerComponent {
    @Input({ required: true }) public component!: EdgeConfig.Component;
    protected readonly Role = Role;
    protected readonly Utils = Utils;
    protected readonly Converter = Converter;
}
