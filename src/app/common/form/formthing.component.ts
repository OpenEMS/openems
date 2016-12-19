import { Connection } from './../../service/connection';
import { FormGroup, FormControl, FormBuilder, Validators } from '@angular/forms';

class ConfigRequest {
  thing: string
  channel: string;
  operation: string;
  value: Object;
}

export class FormThingComponent {

  protected _connection: Connection;
  private _thing: Object;
  private form: FormGroup;

  constructor(
    private _fb: FormBuilder
  ) { }

  public buildForm(thing: Object, disabled?: string[]) {
    this._thing = thing;
    // prepare disabled input fields
    if (!disabled) {
      disabled = [];
    }
    disabled.push("id"); // default
    disabled.push("class"); // default
    // generate input FormGroups
    var controlsConfig: { [key: string]: any; } = {};
    for (let channel in thing) {
      var formState = thing[channel];
      disabled.forEach(d => {
        if (channel == d) {
          formState = { value: formState, disabled: true };
        }
      });
      if (thing[channel] instanceof Array) {
        /* Array */
        var array: FormGroup[] = [];
        for (let config of thing[channel]) {
          if (config["time"]) {
            var group = {};
            group["time"] = config["time"];
            var controllerIds: FormControl[] = [];
            for (let controllerId of config["controllers"]) {
              controllerIds.push(this._fb.control(controllerId));
            }
            group["controllers"] = this._fb.array(controllerIds);
            array.push(this._fb.group(group));
          }
        }
        controlsConfig[channel] = this._fb.array(array);
      } else {
        /* simple string */
        controlsConfig[channel] = [formState, [<any>Validators.required]];
      }
    }
    this.form = this._fb.group(controlsConfig);
  }

  save(form: Object) {
    var configRequests: ConfigRequest[] = [];
    for (let control in this.form.controls) {
      if (this.form.controls[control].dirty) {
        configRequests.push({
          thing: form["controls"]["id"]["_value"],
          channel: control,
          operation: "update",
          value: form["value"][control]
        });
      }
    }
    if (configRequests.length > 0) {
      this._connection.send({
        config: configRequests
      })
    }
  }
}