import { Component, Input } from '@angular/core';
import { Utils } from 'src/app/shared/shared';
import { Role } from 'src/app/shared/type/role';
import { EdgeConfig } from '../../edgeconfig';

@Component({
    selector: "oe-symmetric-meter",
    templateUrl: './modal.component.html'
})
export class SymmetricMeterComponent {

    @Input() public component: EdgeConfig.Component;
    protected readonly Role = Role;
    protected readonly Utils = Utils;
}