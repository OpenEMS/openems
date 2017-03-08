import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { FormControl, FormGroup, FormArray, AbstractControl, FormBuilder } from '@angular/forms';
import { Subscription } from 'rxjs/Subscription';

import { WebsocketService } from '../../../service/websocket.service';
import { WebappService } from '../../../service/webapp.service';
import { Device } from '../../../service/device';
import { AbstractConfig } from '../abstractconfig';
import { AbstractConfigForm, ConfigureRequest, ConfigureUpdateRequest, ConfigureCreateRequest, ConfigureDeleteRequest } from '../abstractconfigform';


@Component({
  selector: 'app-device-config-bridge',
  templateUrl: './bridge.component.html'
})
export class DeviceConfigBridgeComponent extends AbstractConfig {

  private controlConfig: AbstractControl;
  form: FormGroup;
  control: FormGroup;
  device: Device;
  private deviceForms: { [bridge: string]: AbstractControl[] } = {};
  nameReady: boolean = false;
  indexLastBridge: number;
  createdBridge: boolean = false;
  createdDevice: boolean = false;

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
    // console.log(config);
    this.controlConfig = this.buildForm(config);
    // console.log(this.controlConfig);
    this.form = <FormGroup>this.controlConfig;
    // console.log(this.control);
    console.log(this.form);
  }

  setNameReady(): void {
    this.nameReady = true;
  }

  addBridge(bridgeArray: FormArray, $event: any): void {
    if ($event.index == bridgeArray.length && !this.createdBridge) {
      // console.log(controllerArray);
      let group = this.formBuilder.group({
        "id": this.formBuilder.control(""),
        "class": this.formBuilder.control(""),
      });

      group["_meta_new"] = true;
      bridgeArray.push(group);
      this.indexLastBridge = bridgeArray.length - 1;
      this.createdBridge = true;
      // this.createdController = true;
      // console.log(bridgeArray);
      console.log(this.indexLastBridge);
    }
  }

  addDeviceToBridge(bridgeForm: FormGroup, indexParent: number): void {
    if (!this.createdDevice) {
      if (!bridgeForm.controls['devices']) {
        bridgeForm.addControl("devices", this.formBuilder.array([]));
      }

      let deviceArray = <FormArray>bridgeForm.controls['devices'];
      let group = this.formBuilder.group({
        "id": this.formBuilder.control(""),
        "class": this.formBuilder.control(""),
      });

      group["_meta_new"] = true;
      group["_meta_parent"] = bridgeForm;
      group["_meta_parent_id"] = indexParent;
      deviceArray.push(group);
      this.createdDevice = true;
      console.log(bridgeForm);
    }
  }

  addChannelsToBridge(bridgeForm: FormGroup, clazz: string): void {
    let bridgeMeta = <FormArray>this.form.controls['_meta']['controls']['availableBridges'];
    // console.log(bridgeForm);

    for (let indexMeta in bridgeMeta.value) {
      // console.log("First For-Loop // get Index of bridgeMeta");
      // console.log(bridgeMeta.value[indexMeta]);
      // console.log(bridgeForm);
      if (bridgeMeta.value[indexMeta].class == clazz) {
        // console.log("If statement // if both classes equals");
        // console.log(bridgeMeta.value[indexMeta].channels);
        for (let indexChannel in bridgeMeta.value[indexMeta].channels) {
          // console.log("Second For-Loop // get channel of bridgeMeta");
          // console.log(bridgeMeta.value[indexMeta].channels[indexChannel]);

          let channelName = bridgeMeta.value[indexMeta].channels[indexChannel].name;

          bridgeForm.addControl(channelName, this.formBuilder.control(""));

        }

        break;
      }
    }

    this.nameReady = false;
    this.createdBridge = false;
  }

  addNaturesToDevice(deviceForm: FormGroup, clazz: string): void {
    let deviceMeta = <FormArray>this.form.controls['_meta']['controls']['availableDevices'];

    for (let indexMeta in deviceMeta.value) {
      if (deviceMeta.value[indexMeta].class == clazz) {
        for (let indexChannel in deviceMeta.value[indexMeta].channels) {
          let channelName = deviceMeta.value[indexMeta].channels[indexChannel].name;

          deviceForm.addControl(channelName, this.formBuilder.group({
            "id": this.formBuilder.control("")
          }));
        }

        break;
      }
    }

    this.nameReady = false;
    this.createdDevice = false;
    console.log(deviceForm);
  }

  delete(form: FormArray, index: number): void {
    if (form.controls[index]["_meta_new"]) {
      // newly created. No need to delete it at server
      form.removeAt(index);
      form.markAsDirty();
    } else {
      let requests = this.getConfigDeleteRequests(form.controls[index]);
      console.log(requests);
      // this.send(requests);
      // form.markAsPristine();
    }
  }

  save(form: FormArray, index: number): void {
    let requests;
    console.log(form);
    if (form.controls[index]["_meta_new"]) {
      requests = this.getConfigureCreateRequests(form.controls[index]);
    } else {
      requests = this.getConfigureUpdateRequests(form.controls[index]);
    }
    // this.send(requests);
    form["_meta_new"] = false;
    // form.markAsPristine();
  }

  protected getConfigDeleteRequests(form: AbstractControl): ConfigureRequest[] {
    let requests: ConfigureRequest[] = [];
    if (form instanceof FormGroup) {
      requests.push(<ConfigureDeleteRequest>{
        mode: "delete",
        thing: form.controls["id"].value
      });
    }

    return requests;
  }

  protected getConfigureCreateRequests(form: AbstractControl): ConfigureRequest[] {
    let requests: ConfigureRequest[] = [];
    console.log(form);
    // let parentId = form["_meta_parent_id"];
    if (form["_meta_parent_id"] != null) {
      let parentId = form["_meta_parent_id"];

      requests.push(<ConfigureCreateRequest>{
        mode: "create",
        object: this.buildValue(<FormGroup>form),
        parent: parentId
      });
    } else {
      requests.push(<ConfigureCreateRequest>{
        mode: "create",
        object: this.buildValue(<FormGroup>form)
      });
    }

    console.log(requests);
    return requests;
  }

  protected getConfigureUpdateRequests(form: AbstractControl): ConfigureRequest[] {
    let requests: ConfigureRequest[] = [];
    console.log(form);

    if (form instanceof FormGroup) {
      let formControl = form.controls;
      let id = formControl['id'].value;
      for (let key in formControl) {
        if (formControl[key].dirty) {
          // console.log(formControl[key]);
          let value = formControl[key].value;
          // console.log(value, typeof value);
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

  protected buildValue(form: FormGroup): Object {
    let builder: Object = {};
    for (let key in form.controls) {
      // console.log(form.controls[key].value);
      builder[key] = form.controls[key].value;
    }
    return builder;
  }


}