import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { FormControl, FormGroup, FormArray, AbstractControl, FormBuilder } from '@angular/forms';
import { Subscription } from 'rxjs/Subscription';

import { WebsocketService } from '../../service/websocket.service';
import { Device } from '../../service/device';
import { AbstractConfigForm, ConfigureRequest, ConfigureUpdateRequest, ConfigureCreateRequest, ConfigureDeleteRequest } from './abstractconfigform';


export abstract class AbstractConfig implements OnInit {

  protected device: Device;
  private deviceSubscription: Subscription;
  protected config = null;

  constructor(
    private route: ActivatedRoute,
    private websocketService: WebsocketService,
    protected formBuilder: FormBuilder
  ) { }

  protected abstract initForm(config);

  ngOnInit() {
    this.deviceSubscription = this.websocketService.setCurrentDevice(this.route.snapshot.params).subscribe(device => {
      this.device = device;
      if (device && device.config) {
        device.config.subscribe(config => {
          this.config = config;
          this.initForm(config);
        });
      }
    });
  }

  ngOnDestroy() {
    this.deviceSubscription.unsubscribe();
  }

  protected buildForm(item: any, ignoreKeys?: string | string[]): FormControl | FormGroup | FormArray {
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

  protected send(requests: ConfigureRequest[]) {
    if (requests.length > 0) {
      this.device.send({
        configure: requests
      });
    }
  }


}