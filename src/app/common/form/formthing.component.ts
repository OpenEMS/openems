import { Connection } from './../../service/connection';
import { FormGroup, FormControl, FormBuilder, Validators } from '@angular/forms';

export class FormThingComponent {

  protected _form: FormGroup;

  protected setForm(form: FormGroup, disabled: string[]) {
    this._form = form;
    for (let key of disabled) {
      if (form.controls[key] && form.controls[key] instanceof FormControl) {
        var control = form.controls[key];
        control.disable();
      }
    }
  }
}