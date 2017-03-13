import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { FormControl, FormGroup, FormArray, AbstractControl, FormBuilder } from '@angular/forms';
import { Subscription } from 'rxjs/Subscription';

import { WebsocketService } from '../../../service/websocket.service';
import { WebappService } from '../../../service/webapp.service';
import { Device } from '../../../service/device';
import { AbstractConfig } from '../abstractconfig';
import { ConfigureRequest, ConfigureUpdateRequest, ConfigureCreateRequest, ConfigureDeleteRequest } from '../abstractconfigform';

@Component({
    selector: 'app-device-config-controller',
    templateUrl: './controller.component.html'
})

export class DeviceConfigControllerComponent extends AbstractConfig {

    private controlConfig: AbstractControl;
    form: FormGroup;
    control: FormGroup;
    device: Device;
    private deviceForms: { [bridge: string]: AbstractControl[] } = {};
    indexLastController: number;
    nameReady: boolean = false;
    createdController: boolean = false;

    constructor(
        route: ActivatedRoute,
        websocketService: WebsocketService,
        formBuilder: FormBuilder
    ) {
        super(route, websocketService, formBuilder);
    }

    initForm(config) {
        // console.log(config);
        this.controlConfig = this.buildForm(config);
        // console.log(this.controlConfig);
        this.form = <FormGroup>this.controlConfig;
        this.control = this.form.value.scheduler['controllers'];
        // console.log(this.control);
    }

    setNameReady(): void {
        this.nameReady = true;
    }

    isArray(value: any) {
        if (value instanceof Array) {
            return true;
        }
        return false;
    }

    add(channelArray: FormArray): void {
        // console.log(channelArray);
        channelArray.push(this.formBuilder.control(""));
        channelArray.markAsDirty();
    }

    addController(controllerArray: FormArray): void {
        if (!this.createdController) {
            // console.log(controllerArray);
            let group = this.formBuilder.group({
                "id": this.formBuilder.control(""),
                "class": this.formBuilder.control(""),
            });
            // controllerArray.push(this.formBuilder.group({
            //     "id": this.formBuilder.control(""),
            //     "class": this.formBuilder.control("")
            // }));
            controllerArray.markAsDirty();

            group["_meta_new"] = true;
            controllerArray.push(group);
            this.indexLastController = controllerArray.length - 1;
            this.createdController = true;
            console.log(this.indexLastController);
        }
    }

    addChannelsToController(controllerForm: FormGroup, clazz: string): void {
        let controllerMeta = <FormArray>this.form.controls['_meta']['controls']['availableControllers'];
        // console.log(controllerForm);

        for (let indexMeta in controllerMeta.value) {
            // console.log("First For-Loop // get Index of controllerMeta");
            // console.log(controllerMeta.value[indexMeta]);
            // console.log(controllerForm);
            if (controllerMeta.value[indexMeta].class == clazz) {
                // console.log("If statement // if both classes equals");
                // console.log(controllerMeta.value[indexMeta].channels);
                for (let indexChannel in controllerMeta.value[indexMeta].channels) {
                    // console.log("Second For-Loop // get channel of controllerMeta");
                    // console.log(controllerMeta.value[indexMeta].channels[indexChannel]);

                    let channelName = controllerMeta.value[indexMeta].channels[indexChannel].name;

                    if (this.isArray(controllerMeta.value[indexMeta].channels[indexChannel])) {
                        // console.log("Array");
                        controllerForm.addControl(channelName, this.formBuilder.array([]));
                    } else if (!this.isArray(controllerMeta.value[indexMeta].channels[indexChannel])) {
                        // console.log("not Array");
                        controllerForm.addControl(channelName, this.formBuilder.control(""));
                        // console.log(controllerForm);
                    }
                }

                break;
            }
        }

        this.nameReady = false;
        this.createdController = false;
    }

    deleteChannel(indexChannel: number, channelArray: FormArray): void {
        // console.log(channelArray);
        channelArray.removeAt(indexChannel);
        channelArray.markAsDirty();
    }

    protected getConfigureCreateRequests(form: FormGroup): ConfigureRequest[] {
        let requests: ConfigureRequest[] = [];
        // let parentId = "";
        // if (form["_meta_parent_id"]) {
        //     parentId = form["_meta_parent_id"];
        // }
        requests.push(<ConfigureCreateRequest>{
            mode: "create",
            object: this.buildValue(form)
        });

        console.log(requests);
        return requests;
    }

}