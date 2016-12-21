import { Component, Input } from '@angular/core';
import { FormThingComponent } from '../../formthing.component';
import { FormGroup, FormBuilder } from '@angular/forms';

@Component({
  selector: 'form-controller-websocketapi',
  templateUrl: './websocketapi.component.html',
})
export class FormControllerWebsocketApiComponent extends FormThingComponent {

  constructor(
    private formBuilder: FormBuilder
  ) {
    super();
  }

  @Input()
  set form(form: FormGroup) {
    if (!form.value["priority"]) {
      form.addControl("priority", this.formBuilder.control(""));
    }
    if (!form.value["port"]) {
      form.addControl("port", this.formBuilder.control({ value: "", disabled: true }));
    }
    super.setForm(form, []);
  }

}
