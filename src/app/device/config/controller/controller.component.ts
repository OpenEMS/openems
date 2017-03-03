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

    private controlConfig: AbstractControl;
    form: FormGroup;
    control: FormGroup;
    builder: FormBuilder;

    constructor(
        route: ActivatedRoute,
        websocketService: WebsocketService,
        formBuilder: FormBuilder
    ) {
        super(route, websocketService, formBuilder);
        this.builder = formBuilder;
    }

    initForm(config) {
        console.log(config);
        this.controlConfig = this.buildForm(config);
        console.log(this.controlConfig);
        this.form = <FormGroup>this.controlConfig;
        this.control = this.form.value.scheduler['controllers'];
        console.log(this.control);
    }

    isArray(value: any) {
        if (value instanceof Array) {
            return true;
        }
        return false;
    }

    add(channelTitle: String, channelArray: FormArray): void {
        if (channelArray instanceof Array) {
            if (channelTitle == 'Ess') {
                channelArray.push('');
                console.log(channelArray);
                channelArray.markAsDirty();
            } else if (channelTitle == 'Meters') {
                channelArray.push('');
                console.log(channelArray);
                channelArray.markAsDirty();
            }
        }
    }

}