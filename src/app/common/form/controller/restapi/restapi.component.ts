import { Component, Input } from '@angular/core';
import { FormThingComponent } from '../../formthing.component';
import { FormGroup, FormBuilder } from '@angular/forms';

@Component({
  selector: 'form-controller-restapi',
  templateUrl: './restapi.component.html',
})
export class FormControllerRestApiComponent extends FormThingComponent {

  constructor(
    private formBuilder: FormBuilder
  ) {
    super();
  }

  @Input()
  set form(form: FormGroup) {
    if(!form.value["priority"]) {
      form.addControl("priority", this.formBuilder.control(""));
    }
    if(!form.value["port"]) {
      form.addControl("port", this.formBuilder.control(""));
    }
    super.setForm(form, []);
  }
  
}
