import { Component, Input } from '@angular/core';
import { TextIndentation } from 'src/app/shared/genericComponents/modal/modal-line/modal-line';
import { Utils } from 'src/app/shared/shared';
import { Role } from 'src/app/shared/type/role';
import { EdgeConfig } from '../../edgeconfig';

@Component({
    selector: 'oe-asymmetricMeter',
    templateUrl: './modal.component.html'
})
export class AsymmetricMeterComponent {

    @Input() public component: EdgeConfig.Component;
    protected readonly Role = Role;
    protected readonly Utils = Utils;
    public readonly TextIndentation = TextIndentation;
}