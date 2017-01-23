import { AbstractControl, FormGroup, FormControl, FormBuilder, Validators } from '@angular/forms';

import { Device } from '../../service/device';
import { WebsocketService } from '../../service/websocket.service';

export type ConfigRequestType = "update" | "create" | "delete";
export interface ConfigRequest {
  operation: string;
}
export interface ConfigCreateRequest extends ConfigRequest {
  object: Object;
  parentId: string;
}
export interface ConfigUpdateRequest extends ConfigRequest {
  thing: string
  channel: string;
  value: Object;
}
export interface ConfigDeleteRequest extends ConfigRequest {
  thing: string;
}

export abstract class AbstractConfigForm {
  constructor(private websocketService: WebsocketService) {
    websocketService.currentDevice.subscribe(device => {
      this.device = device;
    });
  }

  protected device: Device;
  protected _form: FormGroup;

  protected setForm(form: FormGroup, disabled: string[]) {
    this._form = form;
    for (let key of disabled) {
      let keys = key.split(".");
      if (keys.length == 1) {
        // direct key ("id")
        if (form.controls[key] && form.controls[key] instanceof FormControl) {
          let control = form.controls[key];
          control.disable();
        }
      } else if (keys.length > 1) {
        // object path ("ess.id")
        let parentControl = form.controls[keys[0]];
        if (parentControl && parentControl instanceof FormGroup) {
          let control = parentControl.controls[keys[1]];
          if (control && control instanceof FormControl) {
            control.disable();
          }
        }
      }
    }
  }

  protected save(form: FormGroup) {
    let requests;
    if (form["_meta_new"]) {
      requests = this.getConfigCreateRequests(form);
    } else {
      requests = this.getConfigUpdateRequests(form);
    }
    this.send(requests);
    form["_meta_new"] = false;
    form.markAsPristine();
  }

  protected send(requests: ConfigRequest[]) {
    if (requests.length > 0) {
      this.device.send({
        config: requests
      });
    }
  }

  protected delete(form: FormGroup) {
    if (form["_meta_parent"] && form["_meta_new"]) {
      // newly created. No need to delete it at server
      let array: FormGroup[] = form["_meta_parent"];
      let index: number = array.indexOf(form, 0);
      if (index > -1) {
        array.splice(index, 1);
      }
    } else {
      let requests = this.getConfigDeleteRequests(form);
      this.send(requests);
      form.markAsPristine();
    }
  }

  protected getConfigCreateRequests(form: FormGroup): ConfigRequest[] {
    let requests: ConfigRequest[] = [];
    console.log(form);
    let parentId = "";
    if (form["_meta_parent_id"]) {
      parentId = form["_meta_parent_id"];
    }
    requests.push(<ConfigCreateRequest>{
      operation: "create",
      object: this.buildValue(form),
      parentId: parentId
    });
    return requests;
  }

  protected getConfigUpdateRequests(form: AbstractControl): ConfigRequest[] {
    let requests: ConfigRequest[] = [];
    if (form instanceof FormGroup) {
      for (let key in form.controls) {
        if (form.controls[key].dirty) {
          let value = form.controls[key].value;
          if (typeof value === "object") {
            // value is an object -> call getConfigRequests for sub-object
            return this.getConfigUpdateRequests(form.controls[key]);
          }
          requests.push(<ConfigUpdateRequest>{
            operation: "update",
            thing: form.controls["id"].value,
            channel: key,
            value: value
          });
        }
      }
    }
    return requests;
  }

  protected getConfigDeleteRequests(form: AbstractControl): ConfigRequest[] {
    let requests: ConfigRequest[] = [];
    if (form instanceof FormGroup) {
      requests.push(<ConfigDeleteRequest>{
        operation: "delete",
        thing: form.controls["id"].value
      });
    }
    return requests;
  }

  protected buildValue(form: FormGroup): Object {
    let builder: Object = {};
    for (let key in form.controls) {
      builder[key] = form.controls[key].value;
    }
    console.log(builder);
    return builder;
  }
}