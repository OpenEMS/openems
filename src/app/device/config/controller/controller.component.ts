import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { FormControl, FormGroup, FormArray, AbstractControl, FormBuilder } from '@angular/forms';
import { Subscription } from 'rxjs/Subscription';

import { WebsocketService } from '../../../service/websocket.service';
import { WebappService } from '../../../service/webapp.service';
import { Device } from '../../../service/device';
import { AbstractConfig } from '../abstractconfig';

@Component({
    selector: 'app-device-config-controller',
    templateUrl: './controller.component.html'
})

export class DeviceConfigControllerComponent extends AbstractConfig {

    private form: AbstractControl;

    constructor(
        route: ActivatedRoute,
        websocketService: WebsocketService,
        formBuilder: FormBuilder
    ) {
        super(route, websocketService, formBuilder);
    }

    initForm(config) {
        console.log(config);
        this.form = this.buildForm(config.scheduler);
        console.log(this.form);
    }
}