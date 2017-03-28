import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { FormControl, FormGroup, FormArray, AbstractControl, FormBuilder } from '@angular/forms';
import { Subscription } from 'rxjs/Subscription';

import { WebsocketService, Device } from '../../shared/shared';

export type ConfigureRequestModeType = "update" | "create" | "delete";
export class ConfigureRequest {
  mode: ConfigureRequestModeType;
}
export interface ConfigureUpdateRequest extends ConfigureRequest {
  thing: string
  channel: string;
  value: Object;
}
export interface ConfigureCreateRequest extends ConfigureRequest {
  object: Object;
  parent: string;
}
export interface ConfigureDeleteRequest extends ConfigureRequest {
  thing: string;
}
export interface ConfigureUpdateSchedulerRequest extends ConfigureRequest {
  thing: string
  class: string;
  value: Object;
}

export abstract class AbstractConfigForm {

  protected device: Device;

  constructor(
    protected websocketService: WebsocketService,
  ) {
    websocketService.currentDevice.subscribe(device => {
      this.device = device;
    });
  }

  /**
   * general save() for whole configuration
   */
  protected save(form: FormGroup): void {
    let requests;
    if (form["_meta_new"]) {
      requests = this.getConfigureCreateRequests(form);
      form["_meta_new"] = false;
    } else if (form["_scheduler_new"]) {
      requests = this.getConfigureUpdateSchedulerRequests(form);
      form["_scheduler_new"] = false;
    } else {
      requests = this.getConfigureUpdateRequests(form);
    }

    this.send(requests);
    form.markAsPristine();
  }

  protected abstract getConfigureCreateRequests(form: FormGroup): ConfigureRequest[];

  protected send(requests: ConfigureRequest[]) {
    if (requests.length > 0) {
      this.device.send({
        configure: requests
      });
    }
  }

  protected delete(form: FormArray, index: number): void {
    if (form.controls[index]["_meta_new"]) {
      // newly created. No need to delete it at server
      form.removeAt(index);
      form.markAsDirty();
    } else {
      let requests = this.getConfigDeleteRequests(form.controls[index]);
      // console.log(requests);
      this.send(requests);
      form.markAsPristine();
    }
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

  protected getConfigureUpdateRequests(form: AbstractControl): ConfigureRequest[] {
    let requests: ConfigureRequest[] = [];
    if (form instanceof FormGroup) {
      let formControl = form.controls;
      let id = formControl['id'].value;
      for (let key in formControl) {
        if (formControl[key].dirty) {
          let value = formControl[key].value;

          requests.push(<ConfigureUpdateRequest>{
            mode: "update",
            thing: id,
            channel: key,
            value: value
          });
        }
      }
    }

    // console.log(requests);
    return requests;
  }

  protected getConfigureUpdateSchedulerRequests(form: AbstractControl): ConfigureRequest[] {
    let requests: ConfigureRequest[] = [];
    let builder: Object = {};
    if (form instanceof FormGroup) {
      let formControl = form.controls;
      let id = formControl['id'].value;
      for (let key in formControl) {
        if (formControl[key].dirty) {
          // console.log(formControl[key]);
          let value = formControl[key].value;

          if (key != 'class') {
            builder[key] = value;
          }
        }
      }
      requests.push(<ConfigureUpdateSchedulerRequest>{
        mode: "update",
        thing: id,
        class: formControl['class'].value,
        value: builder
      });
    }

    console.log(requests);
    return requests;
  }

  protected buildValue(form: FormGroup): Object {
    let builder: Object = {};
    for (let key in form.controls) {
      builder[key] = form.controls[key].value;
    }
    return builder;
  }

  /**
   * sets class empty to enable selection of another scheduler
   */
  protected createNewScheduler(schedulerForm: FormGroup) {
    schedulerForm.controls['class'].setValue("");
    schedulerForm.markAsDirty();
  }


}