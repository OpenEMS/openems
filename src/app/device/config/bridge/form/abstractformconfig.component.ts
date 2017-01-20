import { AbstractControl, FormGroup, FormControl, FormBuilder, Validators } from '@angular/forms';

import { Device } from '../../../../service/device';
import { WebsocketService } from '../../../../service/websocket.service';

type ConfigRequestType = "update" | "create" | "delete";
interface ConfigRequest {
  operation: string;
}
interface ConfigCreateRequest extends ConfigRequest {
  object: Object;
  path: string[];
}
interface ConfigUpdateRequest extends ConfigRequest {
  thing: string
  channel: string;
  value: Object;
}
interface ConfigDeleteRequest extends ConfigRequest {
  thing: string;
}

export abstract class AbstractConfigComponent {
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
    let requests = this.getConfigUpdateRequests(form);
    this.send(requests);
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
    console.log("delete", form);
    let requests = this.getConfigDeleteRequests(form);
    console.log(requests);
    this.send(requests);
    form.markAsPristine();
  }

  protected getConfigUpdateRequests(form: AbstractControl): ConfigRequest[] {
    var requests: ConfigRequest[] = [];
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
    var requests: ConfigRequest[] = [];
    if (form instanceof FormGroup) {
      requests.push(<ConfigDeleteRequest>{
        operation: "delete",
        thing: form.controls["id"].value
      });
    }
    return requests;
  }
}