import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { FormControl, FormGroup, FormArray, AbstractControl, FormBuilder } from '@angular/forms';
import { Subscription } from 'rxjs/Subscription';

import { WebsocketService } from '../../../service/websocket.service';
import { WebappService } from '../../../service/webapp.service';
import { Device } from '../../../service/device';
import { AbstractConfig } from '../abstractconfig';
import { AbstractConfigForm, ConfigureRequest, ConfigureUpdateRequest } from '../abstractconfigform';

@Component({
    selector: 'app-device-config-controller',
    templateUrl: './controller.component.html'
})

export class DeviceConfigControllerComponent extends AbstractConfig {

    private controlConfig: AbstractControl;
    form: FormGroup;
    control: FormGroup;
    device: Device;

    constructor(
        route: ActivatedRoute,
        websocketService: WebsocketService,
        formBuilder: FormBuilder
    ) {
        super(route, websocketService, formBuilder);
        websocketService.currentDevice.subscribe(device => {
            this.device = device;
        });
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

    add(channelArray: FormArray): void {
        console.log(channelArray);
        channelArray.push(this.formBuilder.control(""));
    }

    delete(indexChannel: number, channelArray: FormArray): void {
        console.log(channelArray);
        channelArray.removeAt(indexChannel);
    }

    protected save(form: FormGroup, index: number) {
        let requests;
        if (form["_meta_new"]) {
            // requests = this.getConfigureCreateRequests(form);
        } else {
            requests = this.getConfigureUpdateRequests(form, index);
        }
        this.send(requests);
        form["_meta_new"] = false;
        form.markAsPristine();
    }

    protected send(requests: ConfigureRequest[]) {
        if (requests.length > 0) {
            this.device.send({
                configure: requests
            });
        }
    }

    protected getConfigureUpdateRequests(form: AbstractControl, index: number): ConfigureRequest[] {
        let requests: ConfigureRequest[] = [];
        if (form instanceof FormGroup) {
            let formControl = form.controls['scheduler']['controls']['controllers']['controls'][index]['controls'];
            let id = formControl['id'].value;
            for (let key in formControl) {
                if (formControl[key].dirty) {
                    let value = formControl[key].value;
                    console.log(value, typeof value);
                    // if (typeof value === "object") {
                    //     console.log("X");
                    //     // value is an object -> call getConfigureRequests for sub-object
                    //     return this.getConfigureUpdateRequests(formControl[key], index);
                    // }
                    requests.push(<ConfigureUpdateRequest>{
                        mode: "update",
                        thing: id,
                        channel: key,
                        value: value
                    });
                }
            }
        }
        console.log(requests);
        return requests;
    }

}


// {
//     configure: [{
//         mode: "update",
//         thing: "_controller0",
//         channel: "priority",
//         value: "50"
//     }, {
//         mode: "update",
//         thing: "_controller0",
//         channel: "esss",
//         value: ["ess0", "ess1"]
//     }]
// }