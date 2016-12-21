import { Component, Input } from '@angular/core';
import { FormThingComponent } from '../formthing.component';
import { FormGroup, FormBuilder, FormArray, FormControl } from '@angular/forms';
import { Connection } from './../../../service/connection';

interface Day {
  label: string;
  key: string;
  active: boolean;
}

@Component({
  selector: 'form-controllers',
  templateUrl: './controllers.component.html',
})
export class FormControllersComponent {

  constructor(
    private formBuilder: FormBuilder
  ) {
  }

  @Input()
  private form: FormGroup;

  @Input()
  private connection: Connection;

  public addController() {
    console.log("addController", this.form.controls["scheduler"]["controls"]["controllers"]);
    var controllers: FormArray = this.form.controls["scheduler"]["controls"]["controllers"];
    var controller = this.formBuilder.group({
      "id": this.formBuilder.control(""),
      "class": this.formBuilder.control(""),
    });
    controller["_opened"] = true;
    controller["_new"] = true;
    controllers.push(controller);
  }

  public removeController(id: number) {

  }
}
