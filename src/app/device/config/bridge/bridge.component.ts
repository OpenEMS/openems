import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { FormControl, FormGroup, FormArray, AbstractControl, FormBuilder } from '@angular/forms';
import { Subscription } from 'rxjs/Subscription';

import { WebsocketService } from '../../../service/websocket.service';
import { Device } from '../../../service/device';


@Component({
  selector: 'app-device-config-bridge',
  templateUrl: './bridge.component.html'
})
export class DeviceConfigBridgeComponent implements OnInit {

  private device: Device;
  private deviceSubscription: Subscription;

  private config = null;
  private bridgeForms: AbstractControl[] = [];
  private deviceForms: { [bridge: string]: AbstractControl[] } = {};

  constructor(
    private route: ActivatedRoute,
    private websocketService: WebsocketService,
    private formBuilder: FormBuilder
  ) { }

  ngOnInit() {
    this.deviceSubscription = this.websocketService.setCurrentDevice(this.route.snapshot.params).subscribe(device => {
      this.device = device;
      if (device && device.config) {
        device.config.subscribe(config => {
          console.log(config);
          this.config = config;
          this.bridgeForms = [];
          this.deviceForms = {};
          for (let bridge of config.things) {
            // bridge forms
            let bridgeForm = this.buildForm(bridge, "devices");
            this.bridgeForms.push(bridgeForm);
            // device forms
            let deviceForms: any[] = [];
            for (let device of bridge["devices"]) {
              let deviceForm = this.buildForm(device, ["system"]);
              deviceForms.push(deviceForm);
            }
            this.deviceForms[bridge["id"]] = deviceForms;
            console.log("bridgeForm", bridgeForm);
            console.log("deviceForm", deviceForms);
          }
        });
      }
    });
  }

  ngOnDestroy() {
    this.deviceSubscription.unsubscribe();
  }

  private addNewDevice(bridgeFormId: string) {
    let group = this.formBuilder.group({
      "id": this.formBuilder.control(""),
      "class": this.formBuilder.control(""),
    });
    group["_meta_new"] = true;
    group["_meta_parent"] = this.deviceForms[bridgeFormId];
    group["_meta_parent_id"] = bridgeFormId;
    this.deviceForms[bridgeFormId].push(group);
  }

  private buildForm(item: any, ignoreKeys?: string | string[]): FormControl | FormGroup | FormArray {
    if (typeof item === "function") {
      // ignore
    } else if (item instanceof Array) {
      return this.buildFormArray(item, ignoreKeys);
    } else if (item instanceof Object) {
      return this.buildFormGroup(item, ignoreKeys);
    } else {
      return this.buildFormControl(item, ignoreKeys);
    }
  }

  private buildFormGroup(object: any, ignoreKeys?: string | string[]): FormGroup {
    let group: { [key: string]: any } = {};
    for (let key in object) {
      if ((typeof ignoreKeys === "string" && key == ignoreKeys) || (typeof ignoreKeys === "object") && ignoreKeys.some(ignoreKey => ignoreKey === key)) {
        // ignore
      } else {
        var form = this.buildForm(object[key], ignoreKeys);
        if (form) {
          group[key] = form;
        }
      }
    }
    return this.formBuilder.group(group);
  }

  private buildFormControl(item: Object, ignoreKeys?: string | string[]): FormControl {
    return this.formBuilder.control(item);
  }

  private buildFormArray(array: any[], ignoreKeys?: string | string[]): FormArray {
    var builder: any[] = [];
    for (let item of array) {
      var control = this.buildForm(item, ignoreKeys);
      if (control) {
        builder.push(control);
      }
    }
    return this.formBuilder.array(builder);
  }
}