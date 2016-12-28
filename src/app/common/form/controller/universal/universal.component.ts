import { Component, Input } from '@angular/core';
import { FormThingComponent } from '../../formthing.component';
import { FormGroup, FormBuilder } from '@angular/forms';
import { Connection } from './../../../../service/connection';

@Component({
  selector: 'form-controller-universal',
  templateUrl: './universal.component.html',
})
export class FormControllerUniversalComponent extends FormThingComponent {

  constructor(
    private formBuilder: FormBuilder
  ) {
    super();
  }

  @Input()
  set form(form: FormGroup) {
    //TODO
    var controllers: Object[] = form.parent.parent.parent.value["_controllers"];
    

    console.log();
/*
    if (!form.value["priority"]) {
      form.addControl("priority", this.formBuilder.control(""));
    }
    if (!form.value["port"]) {
      form.addControl("port", this.formBuilder.control(""));
    }*/
    super.setForm(form, []);
  }

}
