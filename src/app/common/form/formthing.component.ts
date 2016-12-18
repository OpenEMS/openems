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
      console.log("formstate", formState);
      disabled.forEach(d => {
        if (channel == d) {
          formState = { value: formState, disabled: true };
        }
      });
      if (thing[channel] instanceof Object) {
        /* object */
        var group: { [key: string]: any; } = {};
        for (let time in thing[channel]) {
          console.log(time, thing[channel][time]);
          var controls: any[] = [];
          for(let controllerId of thing[channel][time]) {
            controls.push(controllerId);
          }
          group[time] = this._fb.array(controls);
        }
        controlsConfig[channel] = this._fb.group(group);
      } else {
        /* simple string */
        controlsConfig[channel] = [formState, [<any>Validators.required]];
      }
    }
    this.form = this._fb.group(controlsConfig);
    console.log(this.form);
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