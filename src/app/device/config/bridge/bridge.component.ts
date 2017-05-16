import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { FormControl, FormGroup, FormArray, AbstractControl, FormBuilder } from '@angular/forms';
import { Subscription } from 'rxjs/Subscription';

import { WebsocketService, WebappService, Device } from '../../../shared/shared';
import { AbstractConfig, ConfigureRequest, ConfigureUpdateRequest, ConfigureCreateRequest, ConfigureDeleteRequest } from '../abstractconfig';


@Component({
  selector: 'bridge',
  templateUrl: './bridge.component.html'
})
export class BridgeComponent extends AbstractConfig {

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
  }

  initForm(config) {
    // console.log(config);
    this.controlConfig = this.buildForm(config);
    // console.log(this.controlConfig);
    this.form = <FormGroup>this.controlConfig;
    // console.log(this.control);
    console.log(this.form);
    this.checkDeviceChannel();
  }

  checkDeviceChannel(): void {
    let metaDevice = this.form.controls['_meta']['controls']['availableDevices']['controls'];
    let thingBridge = this.form.controls['things']['controls'];

    for (let bridge of thingBridge) {
      for (let bridgeDevice of bridge.controls['devices']['controls']) {
        console.log(bridgeDevice);
        this.addNaturesToDevice(bridgeDevice, bridgeDevice.controls['class'].value);
      }
    }
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
      // console.log(bridgeArray);
      bridgeArray.push(group);
      bridgeArray.markAsDirty();
      this.indexLastBridge = bridgeArray.length - 1;
      this.createdBridge = true;
      // this.createdController = true;
      // console.log(bridgeArray);
      // console.log(this.indexLastBridge);
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
      // console.log(bridgeForm);
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
    // console.log(deviceForm);
  }

  protected getConfigureCreateRequests(form: FormGroup): ConfigureRequest[] {
    let requests: ConfigureRequest[] = [];
    // console.log(form);
    // let parentId = form["_meta_parent_id"];
    if (form["_meta_parent_id"] != null) {
      let parentId = form["_meta_parent_id"];

      requests.push(<ConfigureCreateRequest>{
        mode: "create",
        object: this.buildValue(<FormGroup>form),
        parent: "" + parentId
      });
    } else {
      requests.push(<ConfigureCreateRequest>{
        mode: "create",
        object: this.buildValue(<FormGroup>form)
      });
    }

    // console.log(requests);
    return requests;
  }


}