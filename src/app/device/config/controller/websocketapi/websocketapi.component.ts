import { Component, Input } from '@angular/core';
import { AbstractControl, FormArray, FormGroup, FormBuilder } from '@angular/forms';

import { WebsocketService } from '../../../../service/websocket.service';
import { AbstractConfigForm, ConfigureRequest, ConfigureUpdateRequest } from '../../abstractconfigform';

@Component({
    selector: 'device-config-ctrl-websocketapi',
    templateUrl: './websocketapi.component.html',
})

export class DeviceConfigCtrlWebsocketApiComponent extends AbstractConfigForm {
    constructor(
        websocketService: WebsocketService,
        private formBuilder: FormBuilder
    ) {
        super(websocketService);
    }

    @Input()
    set form(ctrl: FormGroup) {
        console.log(ctrl);
        this._form = ctrl;
    }
}