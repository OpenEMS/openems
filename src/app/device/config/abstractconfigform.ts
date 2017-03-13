import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { FormControl, FormGroup, FormArray, AbstractControl, FormBuilder } from '@angular/forms';
import { Subscription } from 'rxjs/Subscription';

import { WebsocketService } from '../../service/websocket.service';
import { Device } from '../../service/device';

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

export abstract class AbstractConfigForm {

  protected device: Device;

  constructor(
    protected websocketService: WebsocketService,
  ) {
    websocketService.currentDevice.subscribe(device => {
      this.device = device;
    });
  }

  protected save(form: FormGroup): void {
    let requests;
    if (form["_meta_new"]) {
      requests = this.getConfigureCreateRequests(form);
      form["_meta_new"] = false;
    } else {
      requests = this.getConfigureUpdateRequests(form);
    }
    // this.send(requests);
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
      console.log(requests);
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
      builder[key] = form.controls[key].value;
    }
    return builder;
  }


}