import { OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormControl, FormGroup, FormArray, FormBuilder } from '@angular/forms';

import { Websocket } from '../../../shared/shared';
import { AbstractConfigForm } from './abstractconfigform';

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

export abstract class AbstractConfig extends AbstractConfigForm implements OnInit {

  protected config = null;
  protected _form: FormGroup;

  constructor(
    private route: ActivatedRoute,
    websocket: Websocket,
    protected formBuilder: FormBuilder
  ) {
    super(websocket);
  }

  protected abstract initForm(config);

  ngOnInit() {
    super.ngOnInit();
    // TODO
    // this.websocket.setCurrentEdge(this.route.snapshot.params);
    // this.edge.takeUntil(this.ngUnsubscribe).subscribe(edge => {
    //   if (edge != null) {
    //     edge.config.takeUntil(this.ngUnsubscribe).subscribe(config => {
    //       this.config = config;
    //       this.initForm(config);
    //     });
    //   }
    // });
  }

  ngOnDestroy() {
    super.ngOnDestroy();
  }

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

}