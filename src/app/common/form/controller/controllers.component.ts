import { Component, Input, Output, EventEmitter } from '@angular/core';
import { FormThingComponent } from '../formthing.component';
import { FormGroup, FormBuilder, FormArray, FormControl } from '@angular/forms';

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

  private addController() {
    var controllers: FormArray = this.form.controls["scheduler"]["controls"]["controllers"];
    var controller = this.formBuilder.group({
      "id": this.formBuilder.control(""),
      "class": this.formBuilder.control(""),
    });
    controller["_opened"] = true;
    controller["_new"] = true;
    controllers.push(controller);
  }

  private deleteController(controllers: FormArray, index: number) {
    var controller = controllers.controls[index];
    console.log(controllers, controller);
    if (controller) {
      if (controller["_new"]) {
        controllers.removeAt(index);
      } else {
        controller["_deleted"] = true;
        controller.markAsDirty();
      }
    }
  }
}
