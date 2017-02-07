import { AbstractControl, FormGroup, FormControl, FormBuilder, Validators } from '@angular/forms';

import { Device } from '../../service/device';
import { WebsocketService } from '../../service/websocket.service';

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
      requests = this.getConfigureCreateRequests(form);
    } else {
      requests = this.getConfigureUpdateRequests(form);
    }
    console.log(requests);
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

  protected getConfigureCreateRequests(form: FormGroup): ConfigureRequest[] {
    let requests: ConfigureRequest[] = [];
    let parentId = "";
    if (form["_meta_parent_id"]) {
      parentId = form["_meta_parent_id"];
    }
    requests.push(<ConfigureCreateRequest>{
      mode: "create",
      object: this.buildValue(form),
      parent: parentId
    });
    return requests;
  }

  protected getConfigureUpdateRequests(form: AbstractControl): ConfigureRequest[] {
    let requests: ConfigureRequest[] = [];
    if (form instanceof FormGroup) {
      for (let key in form.controls) {
        if (form.controls[key].dirty) {
          let value = form.controls[key].value;
          if (typeof value === "object") {
            // value is an object -> call getConfigureRequests for sub-object
            return this.getConfigureUpdateRequests(form.controls[key]);
          }
          requests.push(<ConfigureUpdateRequest>{
            mode: "update",
            thing: form.controls["id"].value,
            channel: key,
            value: value
          });
        }
      }
    }
    return requests;
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

  protected buildValue(form: FormGroup): Object {
    let builder: Object = {};
    for (let key in form.controls) {
      builder[key] = form.controls[key].value;
    }
    return builder;
  }
}