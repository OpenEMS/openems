import { Component, Input } from '@angular/core';

import { Controller } from '../controller';
import { TemplateHelper, Device } from '../../../../shared/shared';
import { Role, ROLES } from '../../../../shared/type/role';

@Component({
    selector: 'controllerDetails',
    templateUrl: './details.component.html'
})
export class DetailsComponent {
    @Input()
    public controller: Controller

    @Input()
    set device(device: Device) {
        this._device = device;
        if (device) {
            this.role = device.role;
        } else {
            this.role = ROLES.guest;
        }
    }

    public _device: Device
    public role: Role = ROLES.guest;

    constructor(public tmpl: TemplateHelper) { }
}